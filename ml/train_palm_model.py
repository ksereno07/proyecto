# ═══════════════════════════════════════════════════════════════════
#  ENTRENAMIENTO DEL MODELO DE HOJA DE PALMA (MobileNetV3Small)
# ═══════════════════════════════════════════════════════════════════
#
#  Dataset: "Image Dataset of Oil Palm Leaf Diseases"
#    - Autores: Oky Rahmanto, Veri Julianto, Ahmad Rusadi Arrahimi (2025)
#    - DOI: 10.17632/cp65skff89.1
#    - Licencia: CC BY 4.0
#    - Descarga: https://data.mendeley.com/datasets/cp65skff89/1
#    - 1.583 fotos: 899 "daun sakit" (enferma) y 684 "Daun sehat" (sana)
#
#  Uso:
#    1. Descargar y descomprimir el dataset en ./dataset (con las dos carpetas)
#    2. pip install tensorflow pillow numpy matplotlib scikit-learn
#    3. python train_palm_model.py
#    4. Copiar resultados/palm_disease.tflite a
#       app/src/main/assets/palm_disease.tflite
#
#  Opcional: --grouped separa por caja/karton (test mas estricto, sin
#  fotos del mismo lote en train y test). --epochs N para cambiar epocas.
# ═══════════════════════════════════════════════════════════════════

import argparse
import json
import pathlib
import time

import keras
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import tensorflow as tf
from sklearn.metrics import (accuracy_score, classification_report, confusion_matrix,
                             f1_score, precision_score, recall_score, roc_auc_score)
from sklearn.model_selection import train_test_split

# ─── argumentos (con valores por defecto sensatos) ───
parser = argparse.ArgumentParser()
parser.add_argument("--dataset", default=str(pathlib.Path(__file__).parent / "dataset"))
parser.add_argument("--out", default=str(pathlib.Path(__file__).parent / "resultados"))
parser.add_argument("--grouped", action="store_true", help="split por karton, mas estricto")
parser.add_argument("--epochs", type=int, default=10)
args = parser.parse_args()

# ─── rutas ───
DATA = pathlib.Path(args.dataset)
OUT = pathlib.Path(args.out)
OUT.mkdir(parents=True, exist_ok=True)

IMG_SIZE = 224        # tamaño que espera MobileNetV3
BATCH = 32            # fotos por paso
SEED = 42             # pa' que salga igual cada vez

keras.utils.set_random_seed(SEED)

# ─── 1. listar imagenes con su etiqueta ───
paths, labels = [], []
for folder in DATA.iterdir():
    if not folder.is_dir():
        continue
    name = folder.name.lower()
    if name.startswith("daun sakit"):
        label = 1          # enferma
    elif name.startswith("daun sehat"):
        label = 0          # sana
    else:
        continue
    for p in folder.rglob("*.jpg"):
        paths.append(str(p))
        labels.append(label)

paths = np.array(paths)
labels = np.array(labels, dtype=np.int32)
print(f"total imagenes: {len(paths)}  sanas: {(labels == 0).sum()}  enfermas: {(labels == 1).sum()}")

# ─── 2. split ───
if args.grouped:
    # split por karton: cada caja va completa a train, val o test,
    # asi no se cuelan fotos casi iguales del mismo lote en el test
    groups = np.array([pathlib.Path(p).parent.name for p in paths])
    x_train, y_train, x_val, y_val, x_test, y_test = [], [], [], [], [], []
    for cls in (0, 1):
        idx = np.where(labels == cls)[0]
        cartones = sorted(set(groups[idx]))
        val_g = {cartones[-3]}          # el antepenultimo karton va a validacion
        test_g = set(cartones[-2:])     # los dos ultimos van a test
        for i in idx:
            g = groups[i]
            if g in test_g:
                x_test.append(paths[i]); y_test.append(labels[i])
            elif g in val_g:
                x_val.append(paths[i]); y_val.append(labels[i])
            else:
                x_train.append(paths[i]); y_train.append(labels[i])
    x_train, y_train = np.array(x_train), np.array(y_train, dtype=np.int32)
    x_val, y_val = np.array(x_val), np.array(y_val, dtype=np.int32)
    x_test, y_test = np.array(x_test), np.array(y_test, dtype=np.int32)
else:
    # split estratificado normal 70 / 15 / 15
    x_tmp, x_test, y_tmp, y_test = train_test_split(
        paths, labels, test_size=0.15, stratify=labels, random_state=SEED
    )
    x_train, x_val, y_train, y_val = train_test_split(
        x_tmp, y_tmp, test_size=0.1765, stratify=y_tmp, random_state=SEED
    )
print(f"train: {len(x_train)}  val: {len(x_val)}  test: {len(x_test)}")


# ─── 3. pipeline de datos ───
def load_image(path, label):
    raw = tf.io.read_file(path)
    img = tf.io.decode_jpeg(raw, channels=3)
    img = tf.image.resize(img, (IMG_SIZE, IMG_SIZE))
    return tf.cast(img, tf.float32), label


def make_ds(x, y, training=False, shuffle=False):
    ds = tf.data.Dataset.from_tensor_slices((x, y))
    if shuffle:
        ds = ds.shuffle(len(x), seed=SEED, reshuffle_each_iteration=True)
    ds = ds.map(load_image, num_parallel_calls=tf.data.AUTOTUNE)
    if training:
        # augmentation: solo en entrenamiento, no queda dentro del modelo exportado
        aug = keras.Sequential([
            keras.layers.RandomFlip("horizontal_and_vertical"),
            keras.layers.RandomRotation(0.06),
            keras.layers.RandomZoom(0.12),
            keras.layers.RandomBrightness(0.12),
            keras.layers.RandomContrast(0.12),
        ])
        ds = ds.map(lambda x, y: (aug(x, training=True), y), num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(BATCH).prefetch(tf.data.AUTOTUNE)


ds_train = make_ds(x_train, y_train, training=True, shuffle=True)
ds_val = make_ds(x_val, y_val)
ds_test = make_ds(x_test, y_test)

# class weights por el desbalance 899 vs 684
n0, n1 = (labels == 0).sum(), (labels == 1).sum()
total = n0 + n1
class_weight = {0: total / (2 * n0), 1: total / (2 * n1)}
print("class_weight:", class_weight)

# ─── 4. modelo: MobileNetV3Small + cabeza nueva ───
base_model = keras.applications.MobileNetV3Small(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights="imagenet",
    include_preprocessing=False,
)

inputs = keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3), name="imagen")
# el telefono manda pixeles 0..255 y el modelo los normaliza a [-1, 1] solo
x = keras.layers.Rescaling(2.0 / 255.0, offset=-1.0, name="normalizacion")(inputs)
x = base_model(x, training=False)
x = keras.layers.GlobalAveragePooling2D(name="pool")(x)
x = keras.layers.Dropout(0.25, name="dropout")(x)
outputs = keras.layers.Dense(1, activation="sigmoid", name="sana_o_enferma")(x)

model = keras.Model(inputs, outputs)
print("parametros totales:", model.count_params())

ckpt = OUT / "best_palm.keras"

# ─── 5. fase 1: solo la cabeza, base congelada ───
model.compile(
    optimizer=keras.optimizers.Adam(1e-3),
    loss="binary_crossentropy",
    metrics=["accuracy", keras.metrics.AUC(name="auc")],
)

callbacks = [
    keras.callbacks.ModelCheckpoint(str(ckpt), monitor="val_auc", mode="max", save_best_only=True),
    keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=4, restore_best_weights=True),
    keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.4, patience=2, min_lr=1e-5),
]

t0 = time.time()
print("\n=== FASE 1: cabeza (base congelada) ===")
model.fit(ds_train, validation_data=ds_val, epochs=args.epochs,
          class_weight=class_weight, callbacks=callbacks)

# ─── 6. fase 2: fine-tuning de las ultimas capas ───
print("\n=== FASE 2: fine-tuning ===")
base_model.trainable = True
for layer in base_model.layers[:-40]:
    layer.trainable = False

model.compile(
    optimizer=keras.optimizers.Adam(1e-4),
    loss="binary_crossentropy",
    metrics=["accuracy", keras.metrics.AUC(name="auc")],
)
callbacks2 = [
    keras.callbacks.ModelCheckpoint(str(ckpt), monitor="val_auc", mode="max", save_best_only=True),
    keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=5, restore_best_weights=True),
    keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.4, patience=2, min_lr=1e-6),
]
model.fit(ds_train, validation_data=ds_val, epochs=args.epochs,
          class_weight=class_weight, callbacks=callbacks2)
print(f"entrenamiento: {time.time() - t0:.0f} s")

# ─── 7. evaluacion en test ───
print("\n=== EVALUACION EN TEST ===")
probs = model.predict(ds_test, verbose=0).ravel()
y_true = y_test.astype(int)
y_pred = (probs >= 0.5).astype(int)

metrics = {
    "accuracy": float(accuracy_score(y_true, y_pred)),
    "precision": float(precision_score(y_true, y_pred)),
    "recall": float(recall_score(y_true, y_pred)),
    "f1": float(f1_score(y_true, y_pred)),
    "auc": float(roc_auc_score(y_true, probs)),
    "n_test": int(len(y_true)),
    "confusion_matrix": confusion_matrix(y_true, y_pred).tolist(),
    "class_names": ["sana", "enferma"],
    "split": "grouped" if args.grouped else "random",
}
print(json.dumps(metrics, indent=2))
print(classification_report(y_true, y_pred, target_names=["sana", "enferma"]))
(OUT / "metrics.json").write_text(json.dumps(metrics, indent=2), encoding="utf-8")

# matriz de confusion como imagen
cm = np.array(metrics["confusion_matrix"])
fig, ax = plt.subplots(figsize=(4.5, 4))
ax.imshow(cm, cmap="Greens")
ax.set_xticks([0, 1], ["sana", "enferma"])
ax.set_yticks([0, 1], ["sana", "enferma"])
ax.set_xlabel("prediccion")
ax.set_ylabel("real")
ax.set_title("Matriz de confusion (test)")
for i in range(2):
    for j in range(2):
        ax.text(j, i, str(cm[i, j]), ha="center", va="center", fontsize=14,
                color="white" if cm[i, j] > cm.max() / 2 else "black")
fig.tight_layout()
fig.savefig(OUT / "confusion_matrix.png", dpi=140)
plt.close(fig)

model.save(OUT / "palm_disease.keras")
print("modelo keras guardado")

# ─── 8. convertir a TFLite (cuantizacion dynamic range) ───
print("\n=== CONVERSION TFLITE ===")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()
tflite_path = OUT / "palm_disease.tflite"
tflite_path.write_bytes(tflite_model)
print(f"tflite: {len(tflite_model) / 1024 / 1024:.2f} MB")

# ─── 9. validar que TFLite predice casi igual que Keras ───
interp = tf.lite.Interpreter(model_path=str(tflite_path))
interp.allocate_tensors()
in_det = interp.get_input_details()[0]
out_det = interp.get_output_details()[0]

n_check = min(120, len(x_test))
tfl_probs = []
for p in x_test[:n_check]:
    img = keras.utils.load_img(p, target_size=(IMG_SIZE, IMG_SIZE))
    arr = keras.utils.img_to_array(img)[None, ...].astype(np.float32)
    interp.set_tensor(in_det["index"], arr)
    interp.invoke()
    tfl_probs.append(float(interp.get_tensor(out_det["index"]).ravel()[0]))
tfl_probs = np.array(tfl_probs)

keras_probs = model.predict(make_ds(x_test[:n_check], y_test[:n_check]), verbose=0).ravel()
diff = np.abs(tfl_probs - keras_probs)
agree = ((tfl_probs >= 0.5) == (keras_probs >= 0.5)).mean()
print(f"diferencia media tflite vs keras: {diff.mean():.4f}  acuerdo: {agree * 100:.1f}%")

tfl_metrics = {
    "mean_abs_diff": float(diff.mean()),
    "agreement": float(agree),
    "accuracy_tflite": float(accuracy_score(y_test[:n_check], (tfl_probs >= 0.5).astype(int))),
}
(OUT / "tflite_check.json").write_text(json.dumps(tfl_metrics, indent=2), encoding="utf-8")

print("\nLISTO")
