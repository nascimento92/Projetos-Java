package br.com.ChamadosTI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.jsoup.Jsoup;

import com.sun.mail.imap.protocol.FLAGS;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnLeituraEmailChamdosTI implements AcaoRotinaJava {

	private int contador = 0;
	private String remetente="";
	private String conteudo = "";
	private BigDecimal idProcesso = null;
	private String assunto="";
	
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		try {
			start(linhas,arg0);	
		} catch (Exception e) {
			System.out.println("Nao foi possivel ler os emails! " + e.getMessage());
		}	
	}

	private void start(Registro[] linhas, ContextoAcao arg0) {

		String pop3Host = "outlook.office365.com";// change accordingly
		String mailStoreType = "pop3";
		final String userName = "chamadosti@grancoffee.com.br";// change accordingly
		final String password = "Supgc@2019";// change accordingly
		
		// call receiveEmail
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
		// session.setDebug(true);

		try {
			// Create the POP3 store object and connect to the pop store.
			Store store = session.getStore("pop3s");
			store.connect(pop3Host, userName, password);

			// Create the folder object and open it in your mailbox.
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_WRITE);

			// Retrieve the messages from the folder object.
			Message[] messages = emailFolder.getMessages();
			System.out.println("Total Message: " + messages.length);
			// Iterate the messages
			
			for (int i = 0; i < messages.length; i++) {

				Message message = messages[i];
				BigDecimal id = verificaUltimoID();
				idProcesso = id;
				
				Date dataEmail = message.getSentDate();
				Date dataDeCorte = new Date("12/19/2019"); //mm/dd/yyyy
				
				if (dataEmail.after(dataDeCorte)) {
					if (message != null) {
						verificaEmail(message, id);
						geraChamado();
						this.contador++;
					}
					
					message.setFlag(FLAGS.Flag.DELETED, true);
				} 

			}
			 
			// close the folder and store objects
			System.out.println("Quantidade inserida: "+contador);
			//arg0.setMensagemRetorno("Quantidade: "+contador);
			emailFolder.close(false);
			store.close();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void verificaEmail(Part p, BigDecimal id) throws Exception {

		Object content = p.getContent();
		
			if (p instanceof Message) {
				// Call methos writeEnvelope
				verificaRemetenteAssunto((Message) p, id);
				verificaAnexos(content, id);
			}

			// System.out.println("Type: " + p.getContentType());
			String conteudo = getText(p);
			String plainText = Jsoup.parse(conteudo).text();

			salvaConteudo(id, plainText);
		
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

		// TO
		String to = InternetAddress.toString(m.getRecipients(Message.RecipientType.TO));
		String copia = InternetAddress.toString(m.getRecipients(Message.RecipientType.CC));
		
		this.remetente = remetente.toString();
		this.assunto = assunto;
		
		salvaDados(assunto, remetente, id, data,to,copia);
	}

	public BigDecimal verificaUltimoID() throws Exception {

		BigDecimal count = null;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(ID),0)+1 AS ID FROM AD_EMAILCHAMADOSTI");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("ID");
		}
		return count;

	}
	
	public void salvaDados(String assunto, Address remetente, BigDecimal id, Date data, String to, String copia) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EMAILCHAMADOSTI");
		DynamicVO VO = (DynamicVO) NPVO;

		VO.setProperty("ID", id);
		VO.setProperty("ASSUNTO", assunto);
		VO.setProperty("REMETENTE", remetente.toString());
		VO.setProperty("DTEMAIL", new Timestamp(data.getTime()));
		VO.setProperty("DESTINATARIO", to);
		VO.setProperty("COPIA", copia);

		dwfFacade.createEntity("AD_EMAILCHAMADOSTI", (EntityVO) VO);

	}

	private void verificaAnexos(Object content, BigDecimal id) throws MessagingException, IOException, Exception {
		if (content instanceof Multipart) {

			Multipart multipart = (Multipart) content;

			for (int k = 0; k < multipart.getCount(); k++) {
				BodyPart bodyPart = multipart.getBodyPart(k);

				if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
						&& StringUtils.isBlank(bodyPart.getFileName())) {
					continue; // dealing with attachments only
				}

				InputStream is = bodyPart.getInputStream();
				byte[] bytesFromInputStream = getBytesFromInputStream(is);

				String nome = bodyPart.getFileName();

				salvaAnexo(id, new BigDecimal(k), bytesFromInputStream, nome);
				
			}
		}
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

	private void salvaConteudo(BigDecimal id, String conteudo) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_EMAILCHAMADOSTI", "this.ID=?", new Object[] { id }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;
			
			if(conteudo.length()>4000) {
				conteudo = "Quantidade de caracteres maior que o suportado, não foi possivel ler o conteudo!";
			}
			
			if(conteudo.contains(" De:")) {
				String conteudoEditado = conteudo.substring(0,conteudo.indexOf(" De:"));
				VO.setProperty("CONTEUDO", conteudoEditado);
				this.conteudo = conteudoEditado;
			}else {
				VO.setProperty("CONTEUDO", conteudo);
				this.conteudo = conteudo;
			}
	
			itemEntity.setValueObject(NVO);
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
	
	//gerar chamado TI
	private void geraChamado() throws Exception {
		
		String emailSolicitante = this.remetente;
		BigDecimal usuario = VerificaUsuario(emailSolicitante);
		String problema = this.conteudo;
		BigDecimal tipo = new BigDecimal(012001000);
		
		if(usuario==null) {
			usuario = new BigDecimal(2340);
		}
		BigDecimal numos = gerarCabecalhoOS(usuario,problema);
		geraItemOS(numos,tipo);
		
		if(numos!=null) {
			salvarAnexo(numos,usuario);
			enviarEmail(numos,emailSolicitante);
			salvaDadosChamadosTI(usuario,problema,numos,tipo);
		}
	}
	
	private BigDecimal VerificaUsuario(String solicitante) throws Exception {
		String solicitanteEditado = solicitante.substring(solicitante.indexOf("<")+1,solicitante.lastIndexOf(">"));
		BigDecimal codusu = null;		
		JdbcWrapper jdbcWrapper = null;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODUSU FROM TSIUSU WHERE EMAIL='"+solicitanteEditado+"' AND ROWNUM=1");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			codusu = contagem.getBigDecimal("CODUSU");
		}
		
		return codusu;
	}
	
	private BigDecimal gerarCabecalhoOS(BigDecimal usuario, String problema){
		
		BigDecimal numos = BigDecimal.ZERO;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(414937));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
			
			  NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			  NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,new BigDecimal(7)));
			  NotaProdVO.setProperty("NUMOS",null); 
			  NotaProdVO.setProperty("SITUACAO","P");
			  NotaProdVO.setProperty("CODUSUSOLICITANTE",usuario);
			  NotaProdVO.setProperty("CODUSURESP",usuario);
			  NotaProdVO.setProperty("DESCRICAO",problema);
			  NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			  NotaProdVO.setProperty("AD_CHAMADOTI", "S");
			  NotaProdVO.setProperty("CODATEND", usuario);
			  NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			  NotaProdVO.setProperty("AD_TELASAC", "S");
			  NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			  

			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
						
			return numos;

		} catch (Exception e) {
			System.out.println("Problema ao gerar cabecalho da OS!!"+e.getMessage());
			e.printStackTrace();
		}
		
		return numos;
	}
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private void geraItemOS(BigDecimal numos, BigDecimal tipo){
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		BigDecimal tipoSubOS = new BigDecimal(68);
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(414937),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",new BigDecimal(1));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("SERIE", null);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", tipoSubOS);
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(2238));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			System.out.println("Problema ao gerar Item da OS!!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void salvarAnexo(BigDecimal numos, BigDecimal usuario) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ANEXOEMAILCHAMADOSTI","this.ID = ? ", new Object[] { this.idProcesso }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		
		String nome = DynamicVO.asString("NOME");
		Object anexo = DynamicVO.getProperty("ANEXO");
		
			if (anexo != null) {
				salvarAnexoOs(anexo,numos,usuario,nome);
			}
		}
	}
	
	private void salvarAnexoOs(Object anexo, BigDecimal numos, BigDecimal usuario, String nome) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Anexo");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODATA", numos);
		VO.setProperty("TIPO", "W");
		VO.setProperty("DESCRICAO", nome);
		VO.setProperty("ARQUIVO", nome);
		VO.setProperty("CODUSU", usuario);
		VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
		VO.setProperty("TIPOCONTEUDO", "N");
		VO.setProperty("CONTEUDO", anexo);
		VO.setProperty("EDITA", "N");
		VO.setProperty("SEQUENCIA", new BigDecimal(0));
		VO.setProperty("SEQUENCIAPR", new BigDecimal(0));
		
		dwfFacade.createEntity("Anexo", (EntityVO) VO);

	}
	
	private void enviarEmail(BigDecimal numos, String email) throws Exception {
		
		String mensagem = new String();
		
		mensagem = "Prezado,<br/><br/> "
				+ "A sua solicitação referente ao e-mail \""+this.assunto+"\" foi registrada, OS gerada: <b>"+numos+"</b>."
				+ "<br/><br/>Este chamado pode ser acompanhado pela tela <b>CHAMADOS TI</b>."
				+ "<br/><br/>Caso você não possua acesso ao sistema SANKHYA e queira tirar alguma dúvida referente ao seu chamado, enviar um e-mail para: "
				+ "<br/><br/>Assuntos relacionados ao Sankhya/Verti: sistemas@grancoffee.com.br;"
				+ "<br/><br/>Assuntos relacionados a equipamentos (computadores, celulares, etc...): infra@grancoffee.com.br;"
				+ "<br/><br/><b>IMPORTANTE</b> insira no assunto do e-mail o número do seu chamado: "+numos
				+ "<br/><br/><b>Esta é uma menssagem automática, por gentileza não responder !!</b>"
				+ "<br/><br/>Atencionamente,"
				+ "<br/>Departamento TI"
				+ "<br/>Gran Coffee Comércio, Locação e Serviços S.A."
				+ "<br/>"
				+ "<img src=http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-pq.png  alt=\"\"/>";
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODFILA", getUltimoCodigoFila());
		VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
		VO.setProperty("MENSAGEM", mensagem.toCharArray());
		VO.setProperty("TIPOENVIO", "E");
		VO.setProperty("ASSUNTO", new String("CHAMADO - "+numos));
		VO.setProperty("EMAIL", email);
		VO.setProperty("CODUSU", new BigDecimal(0));
		VO.setProperty("STATUS", "Pendente");
		VO.setProperty("CODCON", new BigDecimal(0));		
		
		dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
	
	private void salvaDadosChamadosTI(BigDecimal usuario, String problema, BigDecimal numos, BigDecimal tipo) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_CHAMADOSTI");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("ID", getUltimoIdChamdosTI());
		VO.setProperty("CODUSU", usuario);
		VO.setProperty("ABERTAEMAIL", "S");
		VO.setProperty("CONTATO", this.remetente);
		VO.setProperty("DESCRICAO", problema);
		VO.setProperty("DTSOLICITACAO", new Timestamp(System.currentTimeMillis()));
		VO.setProperty("NUMOS", numos);
		VO.setProperty("STATUS", "PENDENTE");
		VO.setProperty("TIPO", tipo);
		
		dwfFacade.createEntity("CertificacaoRegra", (EntityVO) VO);

	}
	
	private BigDecimal getUltimoIdChamdosTI() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(ID)+1 AS ID FROM AD_CHAMADOSTI");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("ID");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
}
