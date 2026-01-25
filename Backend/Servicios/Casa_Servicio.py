from Modelos.Dispositivo import Dispositivo, EstadoDispositivo
from Modelos import db

class CasaServicio:
    @staticmethod
    def cambiar_led(db, propiedad_id, habitacion=None, accion=None):
        if accion not in ["encender", "apagar", "todos_encender", "todos_apagar"]:
            return {"error": "Acción no válida"}

        query = db.query(Dispositivo).filter_by(tipo="led", propiedad_id=propiedad_id)
        if habitacion:
            query = query.filter_by(habitacion=habitacion)
        dispositivos = query.all()
        if not dispositivos:
            return {"error": "Habitacion no encontrada o sin LEDs"}

        nuevo_estado = "encendido" if accion in ["encender", "todos_encender"] else "apagado"
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
        Devuelve el estado actual de todos los LEDs por habitación.
        """
        dispositivos = db.query(Dispositivo).filter_by(tipo="led", propiedad_id=propiedad_id).all()
        resultado = {}
        for disp in dispositivos:
            estado = db.query(EstadoDispositivo).filter_by(dispositivo_id=disp.id).first()
            resultado[disp.habitacion] = estado.estado if estado else "apagado"
        return {"estado": resultado}
