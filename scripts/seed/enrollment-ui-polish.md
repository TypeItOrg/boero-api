# Fixture para verificar la selección de cursos

Sólo para el ambiente local/de prueba de Compose. No es una migración ni se ejecuta al iniciar la API.

## Requisitos y ejecución

- Institución activa y ciclo lectivo abierto o planificado.
- Dataset `boero-2025` ya cargado para esa institución/ciclo (se reutilizan su catálogo y docentes ficticios).
- No crea usuarios ni cambia contraseñas/permisos. Usar una cuenta ficticia existente del demo.

```sh
SEED_INSTITUTION=cboero SEED_YEAR=2026 bash scripts/seed/enrollment-ui-polish.sh
```

El runner usa el servicio local `postgres`, una transacción y el registro de IDs existente, separado por institución + dataset `enrollment-ui-polish`. Las ejecuciones repetidas no sobrescriben registros existentes. No elimina ni limpia datos anteriores.

## Escenario

- Trayecto **Prueba UI · Formación instrumental**, separado de los trayectos existentes.
- Planes A y B con nombres largos y dos niveles cada uno.
- 24 cursos a partir del catálogo demo: lenguaje musical, ensamble y práctica instrumental individual/grupal, con Guitarra y Piano.
- Horarios ficticios consecutivos, clases y cupos. Los cursos instrumentales grupales del nivel 2 del plan B no tienen horarios disponibles, para probar la advertencia de lista de espera.
- Dos documentos ficticios, exigidos al enviar y antes de confirmar. Los requisitos pertenecen al trayecto y se aplican a ambos planes, siguiendo el modelo real del producto.

## Recorrido de navegador

1. Iniciar sesión en el portal institucional local con una cuenta ficticia del demo.
2. Nueva inscripción → Prueba UI · Formación instrumental → Comenzar inscripción.
3. Cursos: comprobar agrupación de planes/ciclo, niveles, selección común y selección instrumental.
4. Marcar un instrumental sin elegir instrumento y avanzar: debe mantenerse en Cursos y enfocar el selector con error.
5. Elegir instrumento, cambiarlo, desmarcar y recargar: comprobar el borrador.
6. Documentación: comprobar ambos requisitos sin cargar datos personales reales.
7. Revisar a 390, 768, 1024 y 1440 px, temas claro/oscuro y navegación por teclado.

La ejecución del fixture y estas comprobaciones manuales no equivalen a una suite automatizada ni prueban envío/confirmación definitiva.
