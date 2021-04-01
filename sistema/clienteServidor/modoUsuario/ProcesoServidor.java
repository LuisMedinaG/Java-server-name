package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.ProcesoServidorNombres.Asa;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.ProcesoServidorNombres.DatosServidor;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.util.Pausador;

/**
 * MODIFICADO PARA LA ACTIVIDAD DE CIERRE #2 POR LUIS HUMBERTO MEDINA GALVAN
 */
public class ProcesoServidor extends Proceso {
	short choiceNum;
	String fileName;

	/**
	 * 
	 */
	public ProcesoServidor(Escribano esc) {
		super(esc);
		start();
	}

	/**
	 * 
	 */
	public void run() {
		imprimeln("Proceso servidor en ejecucion.");
		byte[] solServidor = new byte[1024];
		byte[] resServidor = new byte[1024];

		imprimeln("[INFO ] Registrandose en servidor de nombre.");
		byte[] altaServer = new byte[1024];
		int id_ = dameID();
		short codOpe = 100;
		String name = "Servidor de Archivos";
		String ip;

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			ip = "";
		}

		imprimeln("Empaquetando los datos de alta");
		altaServer[8] = (byte) codOpe;
		altaServer[9] = (byte) (codOpe >> 8);

		altaServer[10] = (byte) (id_);
		altaServer[11] = (byte) (id_ >> 8);
		altaServer[12] = (byte) (id_ >> 16);
		altaServer[13] = (byte) (id_ >> 24);

		byte[] ipByte = ip.getBytes();
		altaServer[14] = (byte) (ip.length());
		System.arraycopy(ipByte, 0, altaServer, 15, ip.length());

		byte[] nameByte = name.getBytes();
		altaServer[100] = (byte) (name.length());
		System.arraycopy(nameByte, 0, altaServer, 101, name.length());

		imprimeln("ENVIANDO MENSAJE PARA DARSE DE ALTA EN SERVIDOR DE NOMBRES");
		Nucleo.send(1, altaServer);

		imprimeln("Esperando respuesta ...");
		Nucleo.receive(dameID(), solServidor);

		byte[] inte = new byte[4];
		System.arraycopy(solServidor, 8, inte, 0, inte.length);
		int idUnique = buildInteger(inte);
		imprimeln("[EXITO] El servidor de nombres ha registrado servidor con ID " + idUnique);

		while (continuar()) {
			imprimeln("[INFO ] SE ESTA INVOCANDO A RECIEVE");
			Nucleo.receive(dameID(), solServidor);

			imprimeln("OBTENIENDO EL ID DEL CLIENTE");
			byte[] idClienteArr = new byte[4];
			System.arraycopy(solServidor, 0, idClienteArr, 0, 4);
			int idCliente = buildInteger(idClienteArr);

			imprimeln("INFO: SACANDO EL CODIGO DE OPERACION");
			byte[] opeCodeShort = new byte[2];
			System.arraycopy(solServidor, 8, opeCodeShort, 0, opeCodeShort.length);
			choiceNum = buildShort(opeCodeShort);
			imprimeln("EL CODIGO DE OPERACION ES: " + choiceNum);

			imprimeln("INFO: SACANDO LOS DATOS");
			byte[] inpDataInteger = new byte[4];
			System.arraycopy(solServidor, 10, inpDataInteger, 0, inpDataInteger.length);
			int size = buildInteger(inpDataInteger);
			fileName = new String(solServidor, 14, size);
			imprimeln("EXITO: LOS DATOS	 DE LA OPERACION SON: " + fileName);

			String reply = "";

			switch (choiceNum) {
			/*
			 * CREAR Modo de uso: Solo es necesario escribir el nombre del archivo que se
			 * desea crear, y que este tenga una extension valida. Ejemplos de nombres
			 * validos: miArchivo.json, baseDeDatos.txt, etc.
			 */
			case 0: {
				imprimeln("Se ha solicitado CREAR un archivo con el nombre: " + fileName);
				try {
					FileWriter fileWriter = new FileWriter(fileName);
					fileWriter.close();
					reply = ("EL ARCHIVO DE NOMBRE " + fileName + " HA SIDO CREADO.");
				} catch (IOException e) {
					e.printStackTrace();
					imprimeln(e.getMessage());
					reply = "ERROR: NO SE PUDO CREAR ARCHIVO: " + fileName + ". " + e.getMessage();
				}
				break;
			}
			/*
			 * ELIMINAR Modo de uso: Se debe proporcionar el nombre completo del archivo con
			 * todo y su extension el cual que se quiere eliminar, si este no existe se
			 * imprimira un mensaje de error al usuario. Ejemplo de nombres validos:
			 * miArchivo.json, baseDeDatos.txt, etc.
			 */
			case 1: {
				imprimeln("Se ha solicitado ELIMINAR un archivo con el nombre: " + fileName);
				File file = new File(fileName);
				if (file.delete()) {
					reply = ("EL ARCHIVO DE NOMBRE " + fileName + " HA SIDO ELIMINADO.");
				} else {
					reply = "ERROR: NO SE PUDO ELIMINAR EL ARCHIVO: " + fileName;
				}
				break;
			}
			/*
			 * LEER Modo de uso: Se debe proporcionar el nombre completo del archivo con
			 * todo y su extension el cual que se quiere lear, si este no existe se
			 * imprimira un mensaje de error al usuario. Ademas se debera escribir un arroba
			 * entre la el nombre del archivo y enseguida la linea que se quiere leer
			 * empezando desde la 1. Ejemplo: miArchivo.txt@2
			 */
			case 2: {
				String[] lineCols = fileName.split("@");
				imprimeln("Se ha solicitado LEER la linea " + lineCols[1] + " el nombre: " + fileName);

				try {
					BufferedReader bReader = new BufferedReader(new FileReader(lineCols[0]));
					int lineNum = Integer.parseInt(lineCols[1]);
					int i = 1;
					String line = "";
					while (i <= lineNum) {
						if (bReader.ready()) {
							line = bReader.readLine();
							if (line == null)
								break;
						}
						i++;
					}
					reply = ("LINEA  " + lineCols[1] + " HA SIDO LEIDA: " + line);
					bReader.close();
				} catch (IOException e) {
					imprimeln(e.getMessage());
					reply = "ERROR: NO SE PUDO LEER EL ARCHIVO: " + fileName;
				}
				break;
			}
			/*
			 * ESCRIBIR Modo de uso: Se debe proporcionar el nombre completo del archivo con
			 * todo y su extension en el cual que se quiere escribir, si este no existe se
			 * imprimira un mensaje de error al usuario. Ademas se debera escribir un arroba
			 * entre la el nombre del archivo y el texto que se desea adjuntar al final del
			 * archivo. Ejemplo: miArchivo.txt@Esta es una nueva linea.
			 */
			case 3: {
				String[] lineCols = fileName.split("@");
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(lineCols[0], true)));
					out.println(lineCols[1]);
					out.close();
					reply = ("EL ARCHIVO DE NOMBRE " + fileName + " HA SIDO ESCRITO.");
				} catch (IOException e) {
					imprimeln(e.getMessage());
					reply = "ERROR: NO SE PUDO LEER EL ARCHIVO: " + fileName;
				}
				break;
			}
			default:
				reply = ("ERROR EN EL CODIGO DE OPERACION: " + choiceNum);
				imprimeln(reply);
			}

			imprimeln("INFO: PREPARANDO RESPUESTA");
			byte[] e4 = empaqueta(reply.length());
			System.arraycopy(e4, 0, resServidor, 8, e4.length);

			byte[] answArr = reply.getBytes();
			System.arraycopy(answArr, 0, resServidor, 12, answArr.length);

			Pausador.pausa(1000);
			imprimeln("ENVIANDO LA RESPUESTA AL CLIENTE CON ID: " + idCliente);
			Nucleo.send(idCliente, resServidor);
		}

		imprimeln("PREPARANDO PETICION PARA REMOVER SERVIDOR 1");
		byte[] bajaServer = new byte[1024];
		codOpe = 101;
		bajaServer[8] = (byte) codOpe;
		bajaServer[9] = (byte) (codOpe >> 8);

		bajaServer[10] = (byte) (idUnique);
		bajaServer[11] = (byte) (idUnique >> 8);
		bajaServer[12] = (byte) (idUnique >> 16);
		bajaServer[13] = (byte) (idUnique >> 24);

		imprimeln("ENVIANDO MENSAJE PARA DARSE DE BAJA EN SERVIDOR DE NOMBRES");
		Nucleo.send(1, bajaServer);
		Pausador.pausa(5000);
	}

	private int getNumFromByteArr(byte[] byteArr, int startArr1, int startArr2, boolean isShort) {
		byte[] auxByteArr = new byte[isShort ? 2 : 4];
		System.arraycopy(byteArr, startArr1, auxByteArr, startArr2, auxByteArr.length);
		if (isShort)
			return buildShort(auxByteArr);
		return buildInteger(auxByteArr);
	}

	private static short buildShort(byte[] packetShort) {
		short unpackedShort = 0;
		short auxShort = 0;

		for (int i = packetShort.length - 1; i >= 0; i--) {
			unpackedShort = (short) (unpackedShort << 8);
			auxShort = packetShort[i];
			auxShort = (short) (auxShort & 0x00FF);
			unpackedShort = (short) (unpackedShort | auxShort);
		}

		return unpackedShort;
	}

	private static int buildInteger(byte[] packetInteger) {
		int unpackedInteger = 0;
		int auxInteger = 0;

		for (int i = 3; i >= 0; i--) {
			unpackedInteger = (unpackedInteger << 8);
			auxInteger = packetInteger[i];
			auxInteger = (auxInteger & 0x000000FF);
			unpackedInteger = (unpackedInteger | auxInteger);
		}

		return unpackedInteger;
	}

	private byte[] empaqueta(int integer) {
		byte[] array = new byte[4];
		for (int i = 0; i < array.length; i++) {
			array[i] = (byte) integer;
			integer = (integer >>> 8);

		}
		return array;
	}
}
