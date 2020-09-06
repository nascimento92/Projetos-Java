package br.com.ChamadosTI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.jsoup.Jsoup;

import com.sun.mail.imap.protocol.FLAGS;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class acaoLeituraEmailChamadosTI implements ScheduledAction {
	
	private int contador = 0;
	
	public JapeSession.SessionHandle hnd; 
	//private String emailSolicitante = null;
	//private String assuntoEmail = null;
	//private String dtEmail = null;
	//private String conteudo = null;
	
	public void onTime(ScheduledActionContext arg0) {

		hnd = null;
	
		try {
			
			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {
					start();
				}
				
			});
			
		} catch (Exception e) {
			System.out.println("Nao foi possivel ler os e-mails! "+e.getMessage());
		}	
	}
	
	private void start() {
		
		String pop3Host = "outlook.office365.com";
		String mailStoreType = "pop3";	
		final String userName = "chamadosti@grancoffee.com.br";
		final String password = "Supgc@2019";
		  
		receiveEmail(pop3Host, mailStoreType, userName, password);
	}
	
	public void receiveEmail(String pop3Host,String mailStoreType, String userName, String password){
		
		//Set properties
	    Properties props = new Properties();
	    props.put("mail.store.protocol", "pop3");
	    props.put("mail.pop3.host", pop3Host);
	    props.put("mail.pop3.port", "995");
	    props.put("mail.pop3.starttls.enable", "true");
	    
	 // Get the Session object.
	    Session session = Session.getInstance(props);
	    
	    try {
	    	
	    	//Create the POP3 store object and connect to the pop store.
	    	Store store = session.getStore("pop3s");
	    	store.connect(pop3Host, userName, password);
	    	
	    	//Create the folder object and open it in your mailbox.
	    	Folder emailFolder = store.getFolder("INBOX");
	    	emailFolder.open(Folder.READ_WRITE);
	    	
	    	//Retrieve the messages from the folder object.
	    	Message[] messages = emailFolder.getMessages();
	    	System.out.println("Total Message: " + messages.length);
	    	
	    	for (int i = 0; i < messages.length; i++) {
	    		
	    		Message message = messages[i];
	    		BigDecimal id = verificaUltimoID();
	    		
	    		Date dataEmail = message.getSentDate();
	    		Date dataDeCorte = new Date("12/11/2019");
	    		
	    		if(dataEmail.after(dataDeCorte)) {
	    			 if(message!=null) {
		    			 verificaEmail(message, id);
		    			 contador++;
		    		 }
	    		} 
	    		 message.setFlag(FLAGS.Flag.DELETED, true);
	    	}
	    	
	    	System.out.println("E-mails salvos (Chamados TI): "+contador);
	    	emailFolder.close(true);
		 	store.close();
			
		} catch (MessagingException e){
			e.printStackTrace();
		} catch (Exception e) {
		       e.printStackTrace();
		}
	}
	
	public BigDecimal verificaUltimoID() throws Exception {
		
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(ID),1)+1 AS ID FROM AD_EMAILCHAMADOSTI");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("ID");
		}
		return count;

	}
	
	public void verificaEmail(Part p, BigDecimal id) throws Exception {
		 
		Object content = p.getContent(); 
 
		if (p instanceof Message) {
			// Call methos writeEnvelope
			verificaRemetenteAssunto((Message) p, id);
			verificaAnexos(content,id);
		}
		
		//System.out.println("Type: " + p.getContentType());
		String conteudo = getText(p);
		String plainText= Jsoup.parse(conteudo).text();
		
		salvaConteudo(id,plainText);
	}
	
	public void verificaRemetenteAssunto(Message m, BigDecimal id) throws Exception {
	      
		  Address remetente;
	      String assunto = new String();
	      Date data = m.getSentDate();
	      

	      // FROM
	      if ((remetente = m.getFrom()[0]) != null) {
	         remetente = m.getFrom()[0];
	      }

	      // SUBJECT
	      if (m.getSubject() != null) {
	    	  assunto = m.getSubject();
	      }
	      
	      //TO
	     // Address[] to = m.getRecipients(Message.RecipientType.TO);
	     // Address[] cc = m.getRecipients(Message.RecipientType.CC);
	      String to = InternetAddress.toString(m.getRecipients(Message.RecipientType.TO));
	      String copia = InternetAddress.toString(m.getRecipients(Message.RecipientType.CC));

	      salvaDados(assunto,remetente,id, data, to, copia);
	   }
	
	public void salvaDados(String assunto, Address remetente, BigDecimal id, Date data, String to, String copia) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EMAILCHAMADOSTI");
		DynamicVO VO = (DynamicVO) NPVO;
		
		//emailSolicitante = remetente.toString();
		//assuntoEmail = assunto;
		//dtEmail = com.sankhya.util.StringUtils.formatTimestamp(new Timestamp(data.getTime()), "dd/MM/YYYY");
		
		String emailSolicitanteOriginal = remetente.toString();
		String aux = emailSolicitanteOriginal.substring(emailSolicitanteOriginal.indexOf("<")+1,emailSolicitanteOriginal.lastIndexOf(">"));

		VO.setProperty("ID", id);
		VO.setProperty("ASSUNTO", assunto);
		VO.setProperty("REMETENTE", aux);
		VO.setProperty("DTEMAIL", new Timestamp(data.getTime()));
		VO.setProperty("DESTINATARIO", to);
		VO.setProperty("COPIA", copia);

		dwfFacade.createEntity("AD_EMAILCHAMADOSTI", (EntityVO) VO);
	}
	
	private void verificaAnexos(Object content, BigDecimal id) throws MessagingException, IOException, Exception {
		 if(content instanceof Multipart) {
			 
			 Multipart multipart = (Multipart) content; 
			 
			 for (int k = 0; k < multipart.getCount(); k++) {
		    	 BodyPart bodyPart = multipart.getBodyPart(k);
		    	 
		    	 if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && StringUtils.isBlank(bodyPart.getFileName())) {
		              continue; // dealing with attachments only
		          }
		    	 
		    	 InputStream is = bodyPart.getInputStream();
		    	 byte[] bytesFromInputStream = getBytesFromInputStream(is);
		    	 
		    	 String nome = bodyPart.getFileName();
		    	 
		    	 salvaAnexo(id,new BigDecimal(k),bytesFromInputStream,nome);
		     }
		 }
	}
	
	private String getText(Part p) throws MessagingException, IOException {
		
		boolean textIsHtml = false;
		
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}
	
	private void salvaConteudo(BigDecimal id, String cont) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_EMAILCHAMADOSTI","this.ID=?", new Object[] { id }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("CONTEUDO", cont);

			itemEntity.setValueObject(NVO);
			
			//conteudo ="";
			//conteudo = cont;
		}
	}
	
	public byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}
	
	private void salvaAnexo(BigDecimal id, BigDecimal nroAnexo, byte[] anexo, String nome) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ANEXOEMAILCHAMADOSTI");
		DynamicVO VO = (DynamicVO) NPVO;

		VO.setProperty("ID", id);
		VO.setProperty("NRANEXO", nroAnexo);
		VO.setProperty("ANEXO", anexo);
		VO.setProperty("NOME", nome);

		dwfFacade.createEntity("AD_ANEXOEMAILCHAMADOSTI", (EntityVO) VO);
		
	}

}
