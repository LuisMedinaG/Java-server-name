package sistemaDistribuido.sistema.clienteServidor.modoMonitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;

/**
 * MODIFICADO PARA LA ACTIVIDAD DE CIERRE # POR LUIS HUMBERTO MEDINA GALVAN
 */
public final class MicroNucleo extends MicroNucleoBase {
	private static MicroNucleo nucleo = new MicroNucleo();

	public Hashtable<Integer, ParMaquinaProceso> clientsTable = new Hashtable<Integer, ParMaquinaProceso>();
	public Hashtable<Integer, byte[]> receptionTable = new Hashtable<Integer, byte[]>();

	/** 
	 * 
	 */
	private MicroNucleo() {
	}

	/**
	 * 
	 */
	public final static MicroNucleo obtenerMicroNucleo() {
		return nucleo;
	}

	protected boolean iniciarModulos() {
		return true;
	}

	protected void sendVerdadero(int dest, byte[] message) {
		ParMaquinaProceso pMaqPro = clientsTable.get(dest);

		if (pMaqPro == null) {
			imprimeln("INFO: HACIENDO DIRECCIONAMIENTO DEL SERVIDOR.");
			pMaqPro = dameDestinatarioDesdeInterfaz();
		}
		imprimeln("El proceso invocante es el " + super.dameIdProceso());
		imprimeln("Enviando mensaje a IP=" + pMaqPro.dameIP() + " ID=" + pMaqPro.dameID());

		byte[] idOrgPack = empaqueta(super.dameIdProceso());
		System.arraycopy(idOrgPack, 0, message, 0, idOrgPack.length);
		int entero = buildInteger(idOrgPack);
		imprimeln("INFO: EMPAQUETANDO ID ORIGEN. " + entero);

		byte[] idDesPack = empaqueta(pMaqPro.dameID());
		entero = buildInteger(idDesPack);
		System.arraycopy(idDesPack, 0, message, 4, idDesPack.length);
		imprimeln("INFO: EMPAQUETANDO ID DESTINO. " + entero);

		try {
			imprimeln("> CREANDO EL DATAGRAMPACKET.");
			DatagramPacket dp = new DatagramPacket(message, message.length, InetAddress.getByName(pMaqPro.dameIP()),
					damePuertoRecepcion());

			imprimeln("> OBTENIENDO EL DATAGRAMSOCKET.");
			DatagramSocket socketEmision = dameSocketEmision();
			socketEmision.send(dp);
		} catch (UnknownHostException e1) {
			imprimeln("ERROR: NO SE PUDO CREAR DATAGRAM PACKET. " + e1.getMessage());
		} catch (SocketException e2) {
			imprimeln("ERROR: NO SE PUDO OBTENER DATAGRAM SOCKET. " + e2.getMessage());
		} catch (IOException e3) {
			imprimeln("ERROR: No FUE POSIBLE HACER EL ENVIO. " + e3.getMessage());
		}
	}

	/**
	 * 
	 */
	protected void receiveVerdadero(int addr, byte[] message) {
		imprimeln("INFO: GUARDANDO DATOS EN LA TABLA RECEPCION");
		receptionTable.put(addr, message);

		imprimeln("INFO: SUSPENDIENDO PROCESO");
		suspenderProceso();
	}

	/**
	 * Para el(la) encargad@ de direccionamiento por servidor de nombres en practica
	 * 5
	 */
	protected void sendVerdadero(String dest, byte[] message) {
		imprimeln("[INFO ] EMPAQUETANDO LOS DATOS PARA BUSQUEDA DE SERVIDOR");
		byte[] arregloBusqueda = new byte[1024];
		byte[] respuestaBusqueda = new byte[1024];
		short codOpe = 102;

		arregloBusqueda[8] = (byte) codOpe;
		arregloBusqueda[9] = (byte) (codOpe >> 8);

		byte[] name = dest.getBytes();
		arregloBusqueda[10] = (byte) dest.length();
		System.arraycopy(name, 0, arregloBusqueda, 11, dest.length());

		imprimeln("MANDANDO MENSAJE BUSQUEDA");
		sendVerdadero(1, arregloBusqueda);

		receiveVerdadero(super.dameIdProceso(), respuestaBusqueda);
		imprimeln("LLEGO MENSAJE BUSQUEDA");
		int idServidor;
		String ipServidor;
		Data pmp;

		if (respuestaBusqueda[12] == 0) {
			imprimeln("[ERROR] El servidor de nombres no enontro el servidor.");
			pmp = null;
		} else {
			imprimeln("[EXITO] Obteniendo los datos del servidor.");
			byte[] idClienteArr = new byte[4];
			System.arraycopy(respuestaBusqueda, 0, idClienteArr, 0, 4);
			idServidor = buildInteger(idClienteArr);
			ipServidor = new String(respuestaBusqueda, 13, respuestaBusqueda[12]);

			pmp = new Data(idServidor, ipServidor);
		}

		if (pmp != null) {
			int id = pmp.dameID();
			String ip = pmp.dameIP();

			imprimeln("Ubicacion servidor");
			imprimeln("ID: " + id);
			imprimeln("IP: " + ip);

			int idPro = super.dameIdProceso();
			message[0] = (byte) (idPro);
			message[1] = (byte) (idPro >> 8);
			message[2] = (byte) (idPro >> 16);
			message[3] = (byte) (idPro >> 24);

			message[8] = (byte) (id);
			message[9] = (byte) (id >> 8);
			message[10] = (byte) (id >> 16);
			message[11] = (byte) (id >> 24);

			imprimeln("Enviando mensaje a IP: " + ip + " ID: " + id);

			try {
				imprimeln("> OBTENIENDO EL DATAGRAMSOCKET.");
				DatagramSocket socketEmision = dameSocketEmision();
				imprimeln("> CREANDO EL DATAGRAMPACKET.");
				DatagramPacket dp = new DatagramPacket(message, message.length, InetAddress.getByName(ip),
						damePuertoRecepcion());
				socketEmision.send(dp);
			} catch (UnknownHostException e1) {
				imprimeln("ERROR: NO SE PUDO CREAR DATAGRAM PACKET. " + e1.getMessage());
			} catch (SocketException e2) {
				imprimeln("ERROR: NO SE PUDO OBTENER DATAGRAM SOCKET. " + e2.getMessage());
			} catch (IOException e3) {
				imprimeln("ERROR: No FUE POSIBLE HACER EL ENVIO. " + e3.getMessage());
			}
		} else {
			imprimeln("[ERROR] No hay servidor disponible (MicroNucleo.sendVerdadero:162)");
		}
	}

	/**
	 * Para el(la) encargad@ de primitivas sin bloqueo en practica 5
	 */
	protected void sendNBVerdadero(int dest, byte[] message) {
	}

	/**
	 * Para el(la) encargad@ de primitivas sin bloqueo en practica 5
	 */
	protected void receiveNBVerdadero(int addr, byte[] message) {
	}

	class Data implements ParMaquinaProceso {
		private int _id;
		private String _ip;

		public Data(int id, String ip) {
			_id = id;
			_ip = ip;
		}

		@Override
		public String dameIP() {
			return _ip;
		}

		@Override
		public int dameID() {
			return _id;
		}
	}

	public void run() {
		imprimeln("INFO: OBTENIENDO SOCKETS PARA RECIBIR MENSAJES");
		DatagramSocket socketReception = dameSocketRecepcion();

		imprimeln("> CREANDO DATAGRAM PACKET");
		byte[] buffer = new byte[1024];
		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

		while (seguirEsperandoDatagramas()) {
			try {
				imprimeln("INFO: OBTENIENDO SOCKETS PARA RECIBIR MENSAJES");
				socketReception.receive(dp);

				imprimeln("INFO: LISTO PARA RECIBIR MENSAJES DE LA RED");
				byte[] aux = new byte[4];
				System.arraycopy(buffer, 0, aux, 0, aux.length);
				int origen = buildInteger(aux);

				System.arraycopy(buffer, 4, aux, 0, aux.length);
				int destino = buildInteger(aux);

				String ip = dp.getAddress().getHostAddress();
				imprimeln("PAQUETE DEL ID ORIGEN: " + origen + " QUE VA AL ID DESTINO: " + ip);

				Proceso proceso = dameProcesoLocal(destino);

				if (buffer[1023] == -100) {
					imprimeln("INFO: SE RECIBIO UN PAQUETE AU");
					Proceso proceso2 = dameProcesoLocal(origen);
					reanudarProceso(proceso2);
				} else {
					if (proceso != null) {
						byte[] arr = receptionTable.get(destino);

						if (arr != null) {
							imprimeln("GUARDANDO DATOS DEL ORIGEN");
							Data data = new Data(origen, ip);
							clientsTable.put(origen, data);

							imprimeln("WARNING: BORRANDO DATOS DEL DESTINO EN LA TABLA DE RECEPCION.");
							receptionTable.remove(destino);

							imprimeln("INFO: COPIADNO DATOS AL ESPACIO DESTINO.");
							System.arraycopy(buffer, 0, arr, 0, arr.length);

							reanudarProceso(proceso);
						} else {
							imprimeln("ERROR: NO SE ENCONTRO LA DIRECCION EN LA TABLA.");
						}
					} else {
						imprimeln("ERROR: NO SE ENCONTRO LA DIRECCION DESTINO.");
						buffer[1023] = -100;

						DatagramPacket dp2 = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip),
								damePuertoRecepcion());
						DatagramSocket socketReception2 = dameSocketRecepcion();

						socketReception2.send(dp2);
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static int buildInteger(byte[] packetInteger) {
		int unpackedInteger = 0;
		int auxInteger = 0;

		for (int i = packetInteger.length - 1; i >= 0; i--) {
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
