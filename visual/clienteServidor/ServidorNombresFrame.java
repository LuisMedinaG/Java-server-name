package sistemaDistribuido.visual.clienteServidor;

import java.awt.TextArea;

import sistemaDistribuido.sistema.clienteServidor.modoUsuario.ProcesoServidorNombres;

public class ServidorNombresFrame extends ProcesoFrame {
	private static final long serialVersionUID = 1;
	private ProcesoServidorNombres proc;
	private TextArea areaDatos;

	public ServidorNombresFrame(MicroNucleoFrame frameNucleo) {
		super(frameNucleo, "Servidor de Nombres - Proyecto Final");
		areaDatos = new TextArea();
		areaDatos.setEditable(false);
		add("South", areaDatos);
		proc = new ProcesoServidorNombres(this);
		fijarProceso(proc);
		proc.setArea(areaDatos);
	}
}