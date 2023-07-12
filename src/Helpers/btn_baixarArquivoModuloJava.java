package Helpers;

import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.ws.ServiceContext;

public class btn_baixarArquivoModuloJava implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		// TODO Auto-generated method stub
		SessionHandle hnd = null;
		String extensao = null;
		byte[] fileContent = null;
		SessionFile sessionFile = null;
		
		Registro[] linhas = arg0.getLinhas();
		Registro linha = linhas[0];
		
		String chave = "text_" + UIDGenerator.getNextID();
		String charSet = "UTF-8";
		String nomearquivo = linha.getCampo("DESCRICAO").toString().replace(".JAR", ".jar");
		
		fileContent = (byte[])linha.getCampo("ARQUIVO");
		sessionFile = SessionFile.createSessionFile(nomearquivo, "text", fileContent);
		
		//ServiceContext.getCurrent().putHttpSessionAttribute(chave, sessionFile);
		arg0.setMensagemRetorno("<a id=\"alink\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo=" + chave + "\" target=\"_top\">Arquivo Gerado com Sucesso, clique para Baixar.");
	}

}
