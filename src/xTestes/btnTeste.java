package xTestes;

import java.io.File;
import java.sql.Timestamp;

import org.apache.commons.io.FileUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;

public class btnTeste implements AcaoRotinaJava {
	
	private String nomeInstancia = "arquivos";
	private String chaveMD5 = buildChaveArquivo();
	//private static String PATH_ANEXO = "/Sistema/Anexos/";
	private static String PATH_ANEXO = "/Sistema/workflow/formularios/";
			
	public void doAction(ContextoAcao arg0) throws Exception {
		String name = "Gabriel da Silva Nascimento";
		byte[] nomeCompleto = name.getBytes();
		salvarArquivo(nomeCompleto);
	}
	
	private void salvarArquivo(byte[] data) throws Exception {
		FileUtils.writeByteArrayToFile(new File(SWRepositoryUtils.getBaseFolder() + PATH_ANEXO + nomeInstancia,chaveMD5), data);
	}

	/*
	 * public byte[] getArquivo() throws Exception { return
	 * FileUtils.readFileToByteArray(anexoLocation()); } private File
	 * anexoLocation() { return anexoLocation(this.chaveMD5); } private File
	 * anexoLocation(String chave) { return new
	 * File(buildPathRepo(this.nomeInstancia), chave); }
	 */
	private String buildChaveArquivo() {
		String a = new Timestamp(System.currentTimeMillis()).toString();
		String b = a.replaceAll("[^a-zZ-Z1-9]","");
		return b;
	}
	/*
	 * public static File buildPathRepo(String nomeInstancia) { File
	 * diretorioDestino = new File(SWRepositoryUtils.getBaseFolder() + PATH_ANEXO +
	 * nomeInstancia);
	 * 
	 * if (!diretorioDestino.exists()) { diretorioDestino.mkdir(); }
	 * 
	 * return diretorioDestino; }
	 */

}
