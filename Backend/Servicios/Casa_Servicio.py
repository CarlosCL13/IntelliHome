from Modelos.Dispositivo import Dispositivo, EstadoDispositivo
from Modelos import db

class CasaServicio:
    @staticmethod
    def cambiar_led(db, propiedad_id, habitacion=None, accion=None):
        """
        Cambia el estado de un LED de una habitación o todos, o controla la puerta.
        Para LEDs - accion: "encender", "apagar", "todos_encender", "todos_apagar"
        Para puerta - accion: "abrir", "cerrar"
        """
        # Determinar si es control de puerta o LED
        if accion in ["abrir", "cerrar"]:
            # Control de puerta
            query = db.query(Dispositivo).filter_by(tipo="puerta", propiedad_id=propiedad_id)
            if habitacion:
                query = query.filter_by(habitacion=habitacion)
            dispositivos = query.all()
            if not dispositivos:
                return {"error": "Puerta no encontrada"}
            
            nuevo_estado = "abierto" if accion == "abrir" else "cerrado"
        elif accion in ["encender", "apagar", "todos_encender", "todos_apagar"]:
            # Control de LED
            query = db.query(Dispositivo).filter_by(tipo="led", propiedad_id=propiedad_id)
            if habitacion:
                query = query.filter_by(habitacion=habitacion)
            dispositivos = query.all()
            if not dispositivos:
                return {"error": "Habitacion no encontrada o sin LEDs"}
            
            nuevo_estado = "encendido" if accion in ["encender", "todos_encender"] else "apagado"
        else:
            return {"error": "Acción no válida"}

        for disp in dispositivos:
            estado = db.query(EstadoDispositivo).filter_by(dispositivo_id=disp.id).first()
            if not estado:
                estado = EstadoDispositivo(dispositivo_id=disp.id, estado=nuevo_estado)
                db.add(estado)
            else:
                estado.estado = nuevo_estado
        db.commit()

        return CasaServicio.obtener_estado_leds(db, propiedad_id)

    @staticmethod
    def obtener_estado_leds(db, propiedad_id):
        """
        Devuelve el estado actual de todos los LEDs por habitación y el estado de la puerta.
        """
        # Obtener LEDs
        dispositivos_led = db.query(Dispositivo).filter_by(tipo="led", propiedad_id=propiedad_id).all()
        leds = {}
        for disp in dispositivos_led:
            estado = db.query(EstadoDispositivo).filter_by(dispositivo_id=disp.id).first()
            leds[disp.habitacion] = estado.estado if estado else "apagado"
        
        # Obtener puerta
        dispositivo_puerta = db.query(Dispositivo).filter_by(tipo="puerta", propiedad_id=propiedad_id).first()
        puerta = None
        if dispositivo_puerta:
            estado_puerta = db.query(EstadoDispositivo).filter_by(dispositivo_id=dispositivo_puerta.id).first()
            puerta = {
                "habitacion": dispositivo_puerta.habitacion,
                "estado": estado_puerta.estado if estado_puerta else "cerrado"
            }
        
        return {"estado": leds, "puerta": puerta}
