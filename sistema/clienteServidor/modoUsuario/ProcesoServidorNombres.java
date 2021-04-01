package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import java.awt.TextArea;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoMonitor.ParMaquinaProceso;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.util.Pausador;

/**
 * MODIFICADO PARA PROYECTO FINAL POR LUIS HUMBERTO MEDINA GALVAN
 */
public class ProcesoServidorNombres extends Proceso {
	private Hashtable<Integer, DatosServidor> servideores = new Hashtable<Integer, DatosServidor>();
	private TextArea area;
	private int idUnicoServer = 1;

	public class Asa implements ParMaquinaProceso {
		private int id_;
		private String ip;

		public Asa(int id_, String ip) {
			this.id_ = id_;
			this.ip = ip;
		}

		@Override
		public String dameIP() {
			return ip;
		}

		@Override
		public int dameID() {
			return id_;
		}
	}

	public class DatosServidor {
		private ParMaquinaProceso asa;
		private String name;

		public DatosServidor(ParMaquinaProceso asa, String name) {
			this.asa = asa;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public ParMaquinaProceso dameAsa() {
			return asa;
		}

	}

	public ProcesoServidorNombres(Escribano esc) {
		super(esc);
		start();
	}

	/**
	 * 
	 */
	public void run() {
		imprimeln("Inicio de proceso");
		imprimeln("SERVIDOR DE NOMBRES EN EJECUCION");
		byte[] solServidor = new byte[1024];
		byte[] resServidor = new byte[1024];
		String reply = "";

		imprimeln("EL SERVIDOR DE NOMBRES SE REGISTRARA EN LA TABLA DE EMISION");
		try {
			Nucleo.nucleo.clientsTable.put(dameID(), new Asa(dameID(), InetAddress.getLocalHost().getHostAddress()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		while (continuar()) {
			imprimeln("[INFO ] SE ESTA INVOCANDO A RECIEVE");
			Nucleo.receive(dameID(), solServidor);

			imprimeln("[INFO ] OBTENIENDO EL CODIGO DE OPERACION");
			int codOpe = getNumFromByteArr(solServidor, 8, 0, true);

			switch (codOpe) {
			// -------------------- CREATE SERVER --------------------
			case 100: {
				imprimeln("** Se ha solicitado CREAR un servidor **");

				imprimeln("[INFO ] SACANDO LOS DATOS");
				int id = getNumFromByteArr(solServidor, 10, 0, false);
				String ip = new String(solServidor, 15, solServidor[14]);
				String name = new String(solServidor, 101, solServidor[100]);

				imprimeln("[EXITO] LOS DATOS DEL SERVIDOR SON: ");
				imprimeln("ID: " + id + "Nombre: " + name + " IP: " + ip);

				imprimeln("[INFO ] Registrando los datos ...");
				Asa myAsa = new Asa(id, ip);
				servideores.put(idUnicoServer, new DatosServidor(myAsa, name));

				imprimeln("[INFO ] Preparando respuesta ...");
				resServidor[8] = (byte) (idUnicoServer);
				resServidor[9] = (byte) (idUnicoServer >> 8);
				resServidor[10] = (byte) (idUnicoServer >> 16);
				resServidor[11] = (byte) (idUnicoServer >> 24);
				idUnicoServer++;
				break;
			}
			// -------------------- DELETE SERVER --------------------
			case 101: {
				imprimeln("Se ha solicitado dar de BAJA un servidor");
				int id = getNumFromByteArr(solServidor, 10, 0, false);
				servideores.remove(id);
				break;
			}
			// -------------------- SEARCH SERVER --------------------
			case 102: {
				imprimeln("Se ha solicitado BUSCAR un servidor");
				String name = new String(solServidor, 11, solServidor[10]);
				ParMaquinaProceso pMP = findServer(name);

				if (pMP == null) {
					// No se encontro el servidor
					resServidor[12] = 0;
				} else {
					// Se encontro el servidor
					imprimeln("[INFO ] Preparando respuesta ...");
					idUnicoServer = pMP.dameID();
					resServidor[8] = (byte) (idUnicoServer);
					resServidor[9] = (byte) (idUnicoServer >> 8);
					resServidor[10] = (byte) (idUnicoServer >> 16);
					resServidor[11] = (byte) (idUnicoServer >> 24);

					String ipServer = pMP.dameIP();
					resServidor[12] = (byte) ipServer.length();

					byte[] ipBytes = ipServer.getBytes();
					System.arraycopy(ipBytes, 0, resServidor, 13, ipServer.length());
				}
			}
			// -------------------- -------------- --------------------
			default: {
				reply = ("ERROR EN EL CODIGO DE OPERACION: " + codOpe);
				imprimeln(reply);
			}
			}
			imprimeTabla();
			
			imprimeln("[INFO ] OBTENIENDO EL ID DEL CLIENTE");
			int idCliente = getNumFromByteArr(solServidor, 0, 0, false);

			Pausador.pausa(1000);
			if (codOpe == 100 || codOpe == 102) {
				imprimeln("ENVIANDO LA RESPUESTA AL CLIENTE CON ID: " + idCliente);
				Nucleo.send(idCliente, resServidor);
			}
		}
	}

	private int getNumFromByteArr(byte[] byteArr, int startArr1, int startArr2, boolean isShort) {
		byte[] auxByteArr = new byte[isShort ? 2 : 4];
		System.arraycopy(byteArr, startArr1, auxByteArr, startArr2, auxByteArr.length);
		if (isShort)
			return buildShort(auxByteArr);
		return buildInteger(auxByteArr);
	}

	private ParMaquinaProceso findServer(String name) {
		Enumeration<DatosServidor> list = servideores.elements();

		while (list.hasMoreElements()) {
			DatosServidor ds = (DatosServidor) list.nextElement();

			if (ds.getName().equals(name)) {
				return ds.dameAsa();
			}
		}
		return null;
	}

	private void imprimeTabla() {
		area.setText("");
		Enumeration<DatosServidor> list = servideores.elements();

		while (list.hasMoreElements()) {
			DatosServidor ds = (DatosServidor) list.nextElement();
			area.append("Nombre: " + ds.name + " ID: " + ds.asa.dameID() + " IP:" + ds.asa.dameIP() + "\n");
		}

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

	public void setArea(TextArea areaDatos) {
		area = areaDatos;
	}
}
