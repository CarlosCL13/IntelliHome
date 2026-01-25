import network
import urequests
import time
import machine

# Mapeo de nombre de habitación a pin
habitacion_a_pin = {
    "Sala": machine.Pin(14, machine.Pin.OUT),
    "Cocina": machine.Pin(15, machine.Pin.OUT),
    "Habitacion 1": machine.Pin(16, machine.Pin.OUT),
    "Habitacion 2": machine.Pin(17, machine.Pin.OUT),
    "Habitacion 3": machine.Pin(18, machine.Pin.OUT),
    "Bano 1": machine.Pin(19, machine.Pin.OUT),
    "Bano 2": machine.Pin(20, machine.Pin.OUT),
    "Garaje": machine.Pin(21, machine.Pin.OUT)
}

# Conexión WiFi
SSID = '' # Tu SSID aquí
PASSWORD = '' # Tu contraseña aquí
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.connect(SSID, PASSWORD)
while not wlan.isconnected():
    time.sleep(1)
print("Conectado! IP:", wlan.ifconfig()[0])

# propiedad_id fijo para la maqueta
propiedad_id = 1  # Cambia este valor si tu propiedad es otra

# Función para actualizar LEDs según backend
def actualizar_leds():
    url = f"http://192.168.1.45:8000/casa/estado_leds?propiedad_id={propiedad_id}"
    try:
        response = urequests.get(url)
        data = response.json()
        estados = data["estado"]
        for habitacion, estado in estados.items():
            pin = habitacion_a_pin.get(habitacion)
            if pin:
                pin.value(1 if estado == "encendido" else 0)
        response.close()
    except Exception as e:
        print("Error al consultar backend:", e)

# Loop principal
while True:
    actualizar_leds()
    time.sleep(1)  # Consulta cada segundo