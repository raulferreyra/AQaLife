# AQuaLife
Aplicación móvil para el cálculo de IMC, recomendaciones de hidratación y registro básico de medidas corporales.

## 📱 Descripción
AQuaLife es una aplicación Android desarrollada para brindar al usuario un control sencillo de:
- Índice de Masa Corporal (IMC)
- Recomendación diaria de consumo de agua
- Registro de datos personales (peso, talla, fecha de nacimiento, perímetro abdominal)
- Recordatorios inteligentes de hidratación

La app funciona completamente **offline** y almacena los datos en **SharedPreferences** para su primera versión (Sprint 1).

---

## 🚀 Funcionalidades (Versión del Sprint 1)

### ✔️ Funciones principales
- Cálculo automático del **IMC**
- Recomendación personalizada de **litros de agua por día**
- Avisos periódicos mediante **notificaciones**
- Registro rápido de datos personales
- Pantalla principal con resumen de IMC, datos registrados y recomendaciones
- Perímetro abdominal con detección automática de nivel de riesgo

### ✔️ Recordatorios
- Sistema de alertas usando **AlarmManager**
- Frecuencia configurable
- Cálculo de consumo por notificación (mL → próximamente tazas)

### ✔️ Pantalla de bienvenida (SplashScreen)
- Imagen personalizada centrada
- Fondo blanco limpio
- Optimizada para Android 12+

---

## 🛠️ Tecnologías utilizadas
- **Kotlin**
- **Android Studio**
- **ViewBinding**
- **Navigation Component**
- **Material Design**
- **AlarmManager + PendingIntent**
- **SharedPreferences**

---

## 📦 Instalación del APK
1. Descarga el archivo `AQuaLife.apk` desde la sección *Releases* del repositorio.
2. Cópielo a su dispositivo Android.
3. Habilite: **Configuración → Seguridad → Permitir apps desconocidas**
4. Instale el APK y ejecútelo.

---

## 📚 Roadmap (Siguiente Sprint)

### 🧩 Correcciones
- [X] Agregar Readme
- [X] Corregir Navegación en Setup
- [ ] Ajustar icono final de la app
- [X] Mejorar SplashScreen
- [X] Revisar notificaciones en Android 14
- [ ] Corregir notificaciones (mostrar tazas en vez de mL)
- [X] Agregar campo **Sexo**
- [X] Validación revisada de medidas y pesos
- [X] Validación revisada de fecha de nacimiento
- [X] Validaciones de perímetro abdominal

### 🌟 Nuevas funciones (Sprint 2)
- [X] Gráficos semanales de IMC e hidratación
- [X] Migración a base de datos local (Room DB)
- [X] Sincronización con servidor (BD en la nube)
- [ ] Nuevas medidas corporales
- [X] Login
- [X] Firebase

---

## 📄 Licencia
Este proyecto se distribuye de forma libre para uso académico y personal.  
No está destinado para fines comerciales.

---

## 👨‍💻 Autor
**Raúl Ferreyra**  
Desarrollador – Facultad de Ingeniería de Sistemas  
Autónoma del Perú  

## 👨‍💻 CoAutor
**Ian Zevallos**  
Facultad de Ingeniería de Sistemas  
Autónoma del Perú

## 👨‍💻 CoAutor
**José Santiago**  
Facultad de Ingeniería de Sistemas  
Autónoma del Perú

## 👨‍💻 CoAutor
**Joan Moreno**  
Facultad de Ingeniería de Sistemas  
Autónoma del Perú

## 👨‍💻 CoAutor
**Julio Córdova**  
Facultad de Ingeniería de Sistemas  
Autónoma del Perú  
