import network
import urequests
import time
import machine
from machine import Pin, ADC, Timer, PWM
import gc
import _thread
try:
    import ure as re
except ImportError:
    import re
try:
    from urllib.parse import urlencode
except ImportError:
    def urlencode(params):
        # Simple urlencode for MicroPython
        return '&'.join('{}={}'.format(k, v) for k, v in params.items())

# Mapeo de nombre de habitación a pin
habitacion_a_pin = {
    "Sala": machine.Pin(15, machine.Pin.OUT),
    "Cocina": machine.Pin(19, machine.Pin.OUT),
    "Habitacion 1": machine.Pin(14, machine.Pin.OUT),
    "Habitacion 2": machine.Pin(18, machine.Pin.OUT),
    "Habitacion 3": machine.Pin(17, machine.Pin.OUT),
    "Bano 1": machine.Pin(16, machine.Pin.OUT),
    "Bano 2": machine.Pin(20, machine.Pin.OUT),
    "Garaje": machine.Pin(13, machine.Pin.OUT)
}

# --- SERVO PARA PUERTA ---
servo_pin = PWM(Pin(1))  # Pin GPIO 1 para el servo
servo_pin.freq(50)  # Frecuencia de 50Hz para servos estándar

# Posiciones del servo
SERVO_CERRADO = 1638  # 0° (cerrado)
SERVO_ABIERTO = 4915  # 90° (abierto)
servo_posicion_actual = SERVO_CERRADO  # Iniciar cerrado

# Funciones para controlar el servo
def servo_mover_gradual(posicion_destino, pasos=20, delay_ms=80):
    """Mueve el servo gradualmente de la posición actual a la destino"""
    global servo_posicion_actual
    if servo_posicion_actual == posicion_destino:
        return
    
    diferencia = posicion_destino - servo_posicion_actual
    incremento = diferencia // pasos
    
    for i in range(pasos):
        servo_posicion_actual += incremento
        servo_pin.duty_u16(int(servo_posicion_actual))
        time.sleep_ms(delay_ms)
    
    # Asegurar que llegue exactamente a la posición final
    servo_posicion_actual = posicion_destino
    servo_pin.duty_u16(servo_posicion_actual)

def servo_cerrar():
    """Posición cerrada: 0 grados (gradual)"""
    servo_mover_gradual(SERVO_CERRADO)

def servo_abrir():
    """Posición abierta: 90 grados (gradual)"""
    servo_mover_gradual(SERVO_ABIERTO)

# Conexión WiFi
SSID = 'motoedge60fusion_3124' # Tu SSID aquí
PASSWORD = 'carloscl0103' # Tu contraseña aquí
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.connect(SSID, PASSWORD)
while not wlan.isconnected():
    time.sleep(1)
print("Conectado! IP:", wlan.ifconfig()[0])

# propiedad_id fijo para la maqueta
propiedad_id = 1  # Cambia este valor si tu propiedad es otra

# Endpoint de notificación de eventos
NOTIF_URL = "http://10.128.88.223:8000/notificaciones_eventos/notificacion-evento"

# --- SENSOR DE FLAMA (KY-026) ---
flama_pin = Pin(22, Pin.IN, Pin.PULL_UP)  # Pin digital para sensor de flama


# --- SENSOR DE SISMO (KY-031) ---
sismo_pin = Pin(21, Pin.IN, Pin.PULL_DOWN)  # Usa un pin diferente al de LEDs
sismo_evento_pendiente = False
sismo_ultimo_envio = 0
flama_evento_pendiente = False
flama_ultimo_envio = 0

# Función para notificar evento al backend (se ejecuta en hilo)
def _enviar_notificacion(evento):
    try:
        data = {'propiedad_id': propiedad_id, 'evento': evento}
        payload = urlencode(data)
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        response = urequests.post(NOTIF_URL, data=payload, headers=headers)
        print("Notificado evento:", evento, response.text)
        response.close()
        del response
        gc.collect()
    except Exception as e:
        print("Error notificando evento {}: {}".format(evento, e))
        gc.collect()

# Función wrapper para iniciar notificación en hilo
def notificar_evento(evento):
    try:
        _thread.start_new_thread(_enviar_notificacion, (evento,))
    except Exception as e:
        print("Error al crear hilo para notificación: {}".format(e))


# --- SISMO: interrupción (solo marcar variable, no notificar aquí) ---
def sismo_irq_handler(pin):
    global sismo_evento_pendiente
    sismo_evento_pendiente = True

sismo_pin.irq(trigger=Pin.IRQ_FALLING, handler=sismo_irq_handler)

# --- FLAMA: interrupción (solo marcar variable, no notificar aquí) ---
def flama_irq_handler(pin):
    global flama_evento_pendiente
    flama_evento_pendiente = True

flama_pin.irq(trigger=Pin.IRQ_FALLING, handler=flama_irq_handler)

# --- LOOP PRINCIPAL ---
while True:
    # Notificar sismo si está pendiente y han pasado al menos 5 segundos desde el último envío
    if sismo_evento_pendiente:
        ahora = time.time()
        if ahora - sismo_ultimo_envio > 30:
            print("Sismo detectado (loop principal)")
            notificar_evento("sismo")
            sismo_ultimo_envio = ahora
        sismo_evento_pendiente = False

    # Notificar flama si está pendiente y han pasado al menos 5 segundos desde el último envío
    if flama_evento_pendiente:
        ahora = time.time()
        if ahora - flama_ultimo_envio > 30:
            print("FLAMA DETECTADA (loop principal)")
            notificar_evento("incendio")
            flama_ultimo_envio = ahora
        flama_evento_pendiente = False

    # Actualizar LEDs y puerta
    try:
        url = f"http://10.128.88.223:8000/casa/estado_leds?propiedad_id={propiedad_id}"
        response = urequests.get(url)
        data = response.json()
        
        # Actualizar LEDs
        estados = data["estado"]
        for habitacion, estado in estados.items():
            pin = habitacion_a_pin.get(habitacion)
            if pin:
                pin.value(1 if estado == "encendido" else 0)
        
        # Actualizar puerta (servo)
        if "puerta" in data and data["puerta"]:
            estado_puerta = data["puerta"]["estado"]
            if estado_puerta == "abierto":
                servo_abrir()
            else:
                servo_cerrar()
        
        response.close()
        del response
        gc.collect()
    except Exception as e:
        print("Error al consultar backend:", e)
        gc.collect()

    gc.collect()

    time.sleep(1)
