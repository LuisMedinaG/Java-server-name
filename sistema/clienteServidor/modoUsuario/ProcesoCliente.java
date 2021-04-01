package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.util.Escribano;

/**
 * MODIFICADO PARA LA ACTIVIDAD DE CIERRE #2 POR LUIS HUMBERTO MEDINA GALVAN
 */
public class ProcesoCliente extends Proceso {
	private short choiceNum;
	private String inputData;

	public ProcesoCliente(Escribano esc) {
		super(esc);
		start();
	}

	public void run() {
		Nucleo.suspenderProceso();

		imprimeln("INFO: GENERANDO SOLICITUD");
		byte[] dataClient = new byte[1024];
		byte[] answClient = new byte[1024];

		imprimeln("INFO: GUARDANDO EL CODIGO DE OPERACION: " + choiceNum);
		byte[] opeCodePacket = empaqueta(choiceNum);
		System.arraycopy(opeCodePacket, 0, dataClient, 8, opeCodePacket.length);

		imprimeln("INFO: GUARDANDO DATOS DE LA OPERACION: " + inputData);
		byte[] inpDataPacket = empaqueta(inputData.length());
		System.arraycopy(inpDataPacket, 0, dataClient, 10, inpDataPacket.length);
		byte[] inpDataBytes = inputData.getBytes();
		System.arraycopy(inpDataBytes, 0, dataClient, 14, inpDataBytes.length);

		imprimeln("INFO: INVOCANDO A SEND");
		Nucleo.send("Serv Archivos", dataClient);
		
		imprimeln("INFO: INVOCANDO A RECEIVE");
		Nucleo.receive(dameID(), answClient);
		
		imprimeln("EXITO: SE RECIBIO LA RESPUESTA");
		byte[] inpDataInt = new byte[4];
		System.arraycopy(answClient, 8, inpDataInt, 0, 4);
		int size = buildInteger(inpDataInt);
		
		String answ = new String(answClient, 12, size);		
		imprimeln("> EL SERVIDOR RESPONDIO: " + answ);
	}

	public void getFormAndInputValues(int selectedIndex, String text) {
		choiceNum = (short) selectedIndex;
		inputData = text;
	}

	private byte[] empaqueta(short s) {
		byte[] array = new byte[2];
		array[0] = (byte) s;
		array[1] = (byte) (s >>> 8);
		return array;
	}

	private byte[] empaqueta(int integer) {
		byte[] array = new byte[4];
		for (int i = 0; i < array.length; i++) {
			array[i] = (byte) integer;
			integer = (integer >>> 8);

		}
		return array;
	}
	
	private static short buildShort(byte[] packetShort) {
		short unpackedShort = 0;
		short auxShort = 0;

		for (int i = 1; i >= 0; i--) {
			unpackedShort = (short) (unpackedShort<<8);			
			auxShort = packetShort[i];
			auxShort = (short) (auxShort&0x00FF);
			unpackedShort = (short) (unpackedShort|auxShort);			
		}
		
		return unpackedShort;
	}

	private static int buildInteger(byte[] packetInteger) {
		int unpackedInteger = 0;
		int auxInteger = 0;

		for (int i = 3; i >= 0; i--) {
			unpackedInteger = (unpackedInteger<<8);			
			auxInteger = packetInteger[i];
			auxInteger = (auxInteger&0x000000FF);
			unpackedInteger = (unpackedInteger|auxInteger);
		}

		return unpackedInteger;
	}
}
