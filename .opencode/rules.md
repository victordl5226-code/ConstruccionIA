# Reglas de workflow para el Orquestador

## Auto-avance obligatorio

```
Comando exitoso → avanzar al siguiente paso (máx 3s de pausa)
Comando fallido  → plan B inmediato o reporte con solución
```

## Pre-flight checks obligatorios

Antes de cada `edit`:
```
1. grep "<símbolo>" en src/test/ y src/main/  ← verificar TODOS los usos
2. Si hay N hits, asegurar que el cambio cubre los N
3. Si no cubre todos → ajustar el plan
```

## Pipeline pre-entrega

```
Antes de responder al usuario:
  1. task supervisor con descripción "Revisión final pre-entrega"
  2. Esperar veredicto
  3. Si APROBADO → responder
  4. Si VETADO → corregir y repetir
```

## Heartbeat

Durante operaciones >15s sin output:
```
⏳ [descripción de la tarea] — (N segundos)
```
