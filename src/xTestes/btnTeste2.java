package xTestes;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btnTeste2 implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		
		Registro[] linhas = arg0.getLinhas();
		
		try {
			start(linhas,arg0);
		} catch (Exception e) {
			System.out.println("Nao foi possivel ler os emails! " + e.getMessage());
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) {
		String pop3Host = "outlook.office365.com";// change accordingly
		String mailStoreType = "pop3";
		final String userName = "chamadosti@grancoffee.com.br";// change accordingly
		final String password = "Supgc@2019";// change accordingly
		
		receiveEmail(pop3Host, mailStoreType, userName, password, arg0);
	}
	
	public void receiveEmail(String pop3Host, String mailStoreType, String userName, String password, ContextoAcao arg0) {
		
		// Set properties
		Properties props = new Properties();
		props.put("mail.store.protocol", "pop3");
		props.put("mail.pop3.host", pop3Host);
		props.put("mail.pop3.port", "995");
		props.put("mail.pop3.starttls.enable", "true");
		
		// Get the Session object.
		Session session = Session.getInstance(props);
		
		System.setProperty("mail.mime.decodetext.strict", "false");
		
		try {
			
			Store store = session.getStore("pop3s");
			store.connect(pop3Host, userName, password);
			
			// Create the folder object and open it in your mailbox.
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_WRITE);
			
			// Retrieve the messages from the folder object.
			Message[] messages = emailFolder.getMessages();
			System.out.println("Total Message: " + messages.length);
			
			arg0.setMensagemRetorno("Tamanho da caixa: "+messages.length);
			
			emailFolder.close(true);
			store.close();
			
		} catch (MessagingException e) {
			System.out.println("ERRO NA LEITURA DE EMAILS!");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("ERRO NA LEITURA DE EMAILS!");
			e.printStackTrace();
		}
				
	}

}
