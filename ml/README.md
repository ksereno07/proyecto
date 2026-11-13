# Modelo de IA del escáner (AgroPalma)

Aquí está **cómo se entrenó** el modelo que la app usa para decidir si una hoja de
palma está sana o enferma. El resultado (`.tflite`) ya está dentro del APK en
`app/src/main/assets/palm_disease.tflite`, así que la app **sigue funcionando sin
internet**.

---

## 1. Dataset

| Dato | Valor |
|---|---|
| Nombre | *Image Dataset of Oil Palm Leaf Diseases* |
| Autores | Oky Rahmanto, Veri Julianto, Ahmad Rusadi Arrahimi (2025) |
| DOI | [10.17632/cp65skff89.1](https://doi.org/10.17632/cp65skff89.1) |
| Licencia | **CC BY 4.0** (se debe dar crédito a los autores) |
| Descarga | https://data.mendeley.com/datasets/cp65skff89/1 |
| Imágenes | 1.583 JPG (~132 MB): **899 enfermas** (`daun sakit`) y **684 sanas** (`Daun sehat`) |
| Etiquetas | Binarias: **sana / enferma** (no trae el tipo de enfermedad) |

> El dataset **no va dentro de la app**. Se usa solo en el computador para
> entrenar; lo que se instala en el teléfono es el archivo del modelo (1,08 MB).

---

## 2. Cómo entrenar

```powershell
# 1. Descomprimir el dataset en ml/dataset/
#    (deben quedar las carpetas "daun sakit (899)" y "Daun sehat (684)")
# 2. Instalar dependencias
pip install tensorflow pillow numpy matplotlib scikit-learn
# 3. Entrenar (CPU; ~5 min en un i5-12450H)
#    split aleatorio 70/15/15 -> ml/resultados/split_aleatorio
python ml/train_palm_model.py --out ml/resultados/split_aleatorio

# Recomendado para reportar: split estricto por caja/kartón -> ml/resultados
python ml/train_palm_model.py --grouped
```

El script imprime las métricas, guarda la matriz de confusión y exporta:

- `ml/resultados/palm_disease.keras` → modelo completo
- `ml/resultados/palm_disease.tflite` → el que se copia a `app/src/main/assets/`

---

## 3. Arquitectura

```
Foto 224×224 (píxeles 0..255)
        ↓
MobileNetV3Small preentrenada en ImageNet (transfer learning)
        ↓
GlobalAveragePooling → Dropout 0.25 → Dense(1, sigmoid)
        ↓
Probabilidad de "hoja enferma" (0 a 1)
```

- Fase 1: base congelada, se entrena solo la cabeza (Adam 1e-3).
- Fase 2: fine-tuning de las últimas 40 capas (Adam 1e-4).
- `class_weight` para compensar el desbalance 899 vs 684.
- Aumentación de datos: volteo, rotación, zoom, brillo y contraste.
- Exportación a TFLite con cuantización **dynamic range**.

**Importante:** el modelo solo dice **sana o enferma**. El **tipo** de enfermedad
(pudrición, clorosis, mancha foliar, estrés) lo siguen decidiendo las reglas de
color de `PalmAnalyzer.kt`, que además sirven de respaldo si el modelo no carga.

---

## 4. Resultados

### 4.1 Split estricto por cajón/kartón — el que se reporta (`--grouped`)

El dataset viene en 18 cajas (`Karton ...`). Aquí separo cajas completas: el
modelo entrena con unas y se evalúa con **cajas que nunca vio**. Es la métrica
honesta, porque evita que fotos de la misma hoja queden repartidas entre train y
test.

Test: 354 fotos de las cajas Karton 17, Karton 18, karton8 y karton9.

| Métrica | Valor |
|---|---|
| Exactitud (accuracy) | **91,0 %** |
| Precisión | 96,5 % |
| Recall (enfermas detectadas) | 86,5 % |
| F1 | 0,912 |
| AUC | 0,990 |
| TFLite vs Keras | 98,3 % de acuerdo (diferencia media 0,011) |

Matriz de confusión: 156/162 sanas bien, 166/192 enfermas bien.
Es el modelo que está en `app/src/main/assets/palm_disease.tflite` y el que
aparece en `ml/resultados/`.

> Se pierde sobre todo en **falsos negativos** (26 enfermas que se le escapan).
> Por eso importa la segunda capa: cuando el modelo dice sana pero las reglas de
> color ven daño, la app marca el caso como **dudoso** en vez de dar un “sano”
> limpio. Y como la precisión es alta (96,5 %), casi no genera falsas alarmas.

### 4.2 Split aleatorio 70/15/15 (referencia, más optimista)

Test: 238 fotos al azar.

| Métrica | Valor |
|---|---|
| Exactitud (accuracy) | 97,5 % |
| Precisión | 100 % |
| Recall (enfermas detectadas) | 95,6 % |
| F1 | 0,977 |
| AUC | 0,999 |
| TFLite vs Keras | 100 % de acuerdo (diferencia media 0,006) |

Archivos en `ml/resultados/split_aleatorio/`. Este split puede ser optimista
porque hay varias fotos de la misma hoja repartidas entre train y test; sirve
solo como comparación. Además, las fotos del dataset son hojas recortadas: en
campo, con fondo y otra luz, el modelo puede fallar más. Por eso la app combina
el modelo con las reglas y aclara que es orientativo.

---

## 5. Créditos

> Rahmanto, O.; Julianto, V.; Arrahimi, A. R. (2025). *Image Dataset of Oil Palm
> Leaf Diseases*. Mendeley Data, V1. DOI: 10.17632/cp65skff89.1. Licencia CC BY 4.0.

Modelo entrenado con TensorFlow/Keras y ejecutado en el teléfono con **LiteRT
(TensorFlow Lite)**.
