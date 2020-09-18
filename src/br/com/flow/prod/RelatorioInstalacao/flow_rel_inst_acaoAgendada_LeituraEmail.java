package br.com.flow.prod.RelatorioInstalacao;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import org.apache.commons.io.FileUtils;
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
import br.com.sankhya.modelcore.util.SWRepositoryUtils;

public class flow_rel_inst_acaoAgendada_LeituraEmail implements ScheduledAction {
	
	/**
	 * Ação agendada utilizada para dar inicio ao fluxo flow na base de produção
	 * 
	 * @author gabriel.nascimento
	 * @version 5.3
	 * 
	 */
	
	public JapeSession.SessionHandle hnd; 
	private String resp = null;
	private String http = "localhost:8180";
	//private String http = "172.19.36.6:8180";
	private String codusu = "2379";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String jsessionID = null;
	private String programa = "8"; //para editar na mudança do flow
	private String version = null;
	private static String PATH_ANEXO = "/Sistema/workflow/formularios/";
	private String nomeInstancia = "arquivos";
	private String chaveMD5 = null;
	private String nomeArquivo = null;
	private String conteudo = null;
	private String emailSolicitante = null;
	private String assuntoEmail = null;
	private String dtEmail = null;
	private BigDecimal idflow = null;
	private String nomeAnexo = null;
	private int cont = 0;
	private StringTokenizer token=null;
	
	private String campoQueRecebeAnexoComercial = "EMAIL_ANEXO"; //para editar na mudança do flow
	//private String campoIdDoEmail = "EMAIL_ID"; //para editar na mudança do flow
	private String tarefaDeContratos = "UserTask_1jgv6gi"; //para editar na mudança do flow
	
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
			System.out.println("### FLOW ### - NAO FOI POSSIVEL LER OS E-MAILS! " + e.getMessage());
			e.getStackTrace();
		}
		
	}
	
	private void start() {
		
		  String pop3Host = "outlook.office365.com";
		  String mailStoreType = "pop3";	
		  final String userName = "flow@grancoffee.com.br";
		  final String password = "Info@2015";

		  receiveEmail(pop3Host, mailStoreType, userName, password); //LEITURA DOS E-MAILS
	}
	
	//1.0
	public void receiveEmail(String pop3Host,String mailStoreType, String userName, String password){
		
		//Set properties
	    Properties props = new Properties();
	    props.put("mail.store.protocol", "pop3");
	    props.put("mail.pop3.host", pop3Host);
	    props.put("mail.pop3.port", "995");
	    props.put("mail.pop3.starttls.enable", "true");
	    
	    // Get the Session object.
	    Session session = Session.getInstance(props);
	   //session.setDebug(true);
	    
	    try {
	    	//Create the POP3 store object and connect to the pop store.
	    	Store store = session.getStore("pop3s");
	    	store.connect(pop3Host, userName, password);
	    	
	    	//Create the folder object and open it in your mailbox.
	    	Folder emailFolder = store.getFolder("INBOX");
	    	emailFolder.open(Folder.READ_WRITE);
	    	
	    	//Retrieve the messages from the folder object.
	    	Message[] messages = emailFolder.getMessages();
	    	System.out.println("### FLOW ### - TOTAL DE E-MAILS: " + messages.length);

	    	//Iterate the messages
	    	for (int i = 0; i < messages.length; i++) {
	    		 cont++;
	    		 Message message = messages[i];
	    		 BigDecimal id = null;
	    		 id = verificaUltimoID(); //PEGA O ULTIMO ID DA TELA E-MAILS FLOW

	    		 if(message!=null) {
	    			 verificaEmail(message, id);
	    			 criaTarefaFlow(id);
	    			 insereDadosNoFluxo(id);
	    			 insertAnexoNoFluxoFLow(id);
	    			 enviarEmail(emailSolicitante,idflow);
	    			 //enviarEmailValidacao("gabriel.nascimento@grancoffee.com.br",idflow);
	    			 salvaFluxoNaTelaGerencial();
	    			 
	    			 if(token!=null) {
	    				 if(token.countTokens()>0) {
		    				 verificaEmCopia();
		    			 }
	    			 }	 
	    		 }
	    		 
	    		 resp = null;
	    		 jsessionID = null;
	    		 version = null;
	    		 chaveMD5 = null;
	    		 nomeArquivo = null;
	    		 conteudo = null;
	    		 emailSolicitante = null;
	    		 assuntoEmail = null;
	    		 dtEmail = null;
	    		 idflow = null;
	    		 nomeAnexo = null;
	    		 token = null;
	    		 
	    		 message.setFlag(FLAGS.Flag.DELETED, true); //DELETA O E-MAILS.
	    	}
	    	//close the folder and store objects
	 	   emailFolder.close(true);
	 	   store.close();
	    } catch (MessagingException e){
			System.out.println("### FLOW ### - ERRO NA LEITURA DOS E-MAILS "+e.getMessage());
	    	e.printStackTrace();
		} catch (Exception e) {
			System.out.println("### FLOW ### - ERRO NA LEITURA DOS E-MAILS "+e.getMessage());
		       e.printStackTrace();
		}
	}
	
	//LEITURA DOS E-MAILS
	
	//1.1
	public BigDecimal verificaUltimoID() throws Exception {
		
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(ID)+1,1) AS ID FROM AD_EMAILFLOW");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("ID");
		}
		return count;
	}//OK
	
	//1.2
	public void verificaEmail(Part p, BigDecimal id) throws Exception {
		 
		Object content = p.getContent(); 

		if (p instanceof Message) {
			// Call methos writeEnvelope
			verificaRemetenteAssunto((Message) p, id);
			verificaAnexos(content,id);
		}
		
		//System.out.println("Type: " + p.getContentType());
		String conteudo = getText(p); //VERIFICA CONTEUDO
		String plainText= Jsoup.parse(conteudo).text();
		
		salvaConteudo(id,plainText); //SALVA CONTEUDO
  
	}
	
	//1.2.1
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
	      
	      String copia = InternetAddress.toString(m.getRecipients(Message.RecipientType.CC));
	      
	      if(copia!=null) {
	    	  token = new StringTokenizer(copia, ";"); 
	      }

	      salvaDados(assunto,remetente,id, data, copia); //SALVA OS DADOS NA TELA EMAILS FLOW
	   }
	
	//1.2.1.1
	public void salvaDados(String assunto, Address remetente, BigDecimal id, Date data, String copia) throws Exception {
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EMAILFLOW");
			DynamicVO VO = (DynamicVO) NPVO;
			
			emailSolicitante = remetente.toString();
			assuntoEmail = assunto;
			dtEmail = com.sankhya.util.StringUtils.formatTimestamp(new Timestamp(data.getTime()), "dd/MM/YYYY");
			
			VO.setProperty("ID", id);
			VO.setProperty("ASSUNTO", assunto);
			VO.setProperty("REMETENTE", remetente.toString());
			VO.setProperty("DTEMAIL", new Timestamp(data.getTime()));
			VO.setProperty("COPIA", copia);
			
			dwfFacade.createEntity("AD_EMAILFLOW", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR OS DADOS NA TELA EMAILS FLOW "+e.getMessage());
		}	
	} //OK
	
	//1.2.2
	private void verificaAnexos(Object content, BigDecimal id) throws MessagingException, IOException, Exception {
		 if(content instanceof Multipart) {
			 
			 Multipart multipart = (Multipart) content; 
			 
			 for (int k = 0; k < multipart.getCount(); k++) {
		    	 BodyPart bodyPart = multipart.getBodyPart(k);
		    	 
		    	 if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && StringUtils.isBlank(bodyPart.getFileName())) {
		              continue; // dealing with attachments only
		          }
		    	 
		    	 //bodyPart.setDisposition("form-data");
		    	 //bodyPart.setHeader("Content-Disposition","attachment; filename="+ bodyPart.getFileName());
		    	 
		    	 InputStream is = bodyPart.getInputStream();
		    	 byte[] bytesFromInputStream = getBytesFromInputStream(is);
		    	 
		    	 
		    	 String nome = bodyPart.getFileName();	    	 
		    	 salvaAnexo(id,new BigDecimal(k),bytesFromInputStream,nome); //SALVA ANEXOS NA TELA EMAILS FLOW
		    	 
		    	 nomeAnexo="";
		    	 nomeAnexo = nome;
		     }
		 }
	}
	
	//1.2.2.1
	private void salvaAnexo(BigDecimal id, BigDecimal nroAnexo, byte[] anexo, String nome) throws Exception {
		try {
			salvarArquivo(anexo); //SALVA NO REPOSITÓRIO DE ARQUIVOS
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ANEXOSEMAILFLOW");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ID", id);
			VO.setProperty("NRANEXO", nroAnexo);
			VO.setProperty("ANEXO", anexo);
			VO.setProperty("NOME", nome);
			VO.setProperty("REPOANEXO", chaveMD5);
			
			dwfFacade.createEntity("AD_ANEXOSEMAILFLOW", (EntityVO) VO);	
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O ANEXO NA TELA ANEXOS DO E-MAIL FLOW! "+e.getMessage());
		}
		
	}
	
	//1.2.2.1.1
	private void salvarArquivo(byte[] data) throws Exception {
		try {
			
			chaveMD5 = buildChaveArquivo();
			FileUtils.writeByteArrayToFile(new File(SWRepositoryUtils.getBaseFolder() + PATH_ANEXO + nomeInstancia,chaveMD5), data);
			nomeArquivo="";
			nomeArquivo = chaveMD5;
			
			String PATH_ANEXO_TELA_GERENCIA = "/Sistema/Anexos/AD_GERENCIAINST";
			FileUtils.writeByteArrayToFile(new File(SWRepositoryUtils.getBaseFolder() + PATH_ANEXO_TELA_GERENCIA,chaveMD5), data);
			
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O ARQUIVO NO REPOSITORIO! "+e.getMessage());
		}
		
	}
	
	//1.2.3
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
	
	//1.2.3.1
	private void salvaConteudo(BigDecimal id, String cont) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_EMAILFLOW","this.ID=?", new Object[] { id }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CONTEUDO", cont);

				itemEntity.setValueObject(NVO);
				
				conteudo ="";
				conteudo = cont;
			}
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O CONTEUDO DO EMAIL "+e.getMessage());
		}
		
	}
	
	//CRIA TAREFA FLOW
	
	//1.3
	private void criaTarefaFlow(BigDecimal id) throws Exception {
		
		try {
			
			//PEGA O JSESSION ID
			String url = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.login";
			String request1 = "<serviceRequest serviceName=\"MobileLoginSP.login\">\r\n" +
					  " <requestBody>\r\n" + " <NOMUSU>"+nomeusu+"</NOMUSU>\r\n" +
					  " <INTERNO>"+senha+"</INTERNO>\r\n" + " </requestBody>\r\n" +
					  " </serviceRequest>";
			
			Post_JSON(url,request1); //REQUISICAO POST
			jsessionID = getJssesionId(resp);
			version = pegaVersao().toString();
			//System.out.println("==========> JSESSION ID: "+jsessionID);
			
			//CRIA A REQUISICAO DO FLOW
			
			String emailSolicitanteOriginal = emailSolicitante;
			String aux = emailSolicitanteOriginal.substring(emailSolicitanteOriginal.indexOf("<")+1,emailSolicitanteOriginal.lastIndexOf(">"));
			
			
			String query_url = "http://"+http+"/workflow/service.sbr?serviceName=ListaTarefaSP.startProcess&application=ListaTarefa&mgeSession="+jsessionID;
			//String request2 = "{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":"+programa+",\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_"+programa+"_VERSION_+"+version+"\",\"parentEntity\":\"-99999999\",\"records\":[{\"record\":[{\"name\":\"EMAIL_ID\",\"value\":\""+id.toString()+"\"},{\"name\":\"EMAIL_SOLICITANTE\",\"value\":\""+aux+"\"},{\"name\":\"EMAIL_ASSUNTO\",\"value\":\""+assuntoEmail+"\"},{\"name\":\"EMAIL_DATA\",\"value\":\""+dtEmail+"\"},{\"name\":\"EMAIL_CONTEUDO\",\"value\":\""+conteudo+"\"},{\"name\":\"EMAIL_ANEXO\",\"value\":\"0\"}]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
			String request2 ="{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":"+programa+",\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_"+programa+"VERSION_"+version+"\",\"parentEntity\":\"-99999999\",\"records\":["
					+ "{\"record\":["
					+ "{\"name\":\"EMAIL_ANEXO\",\"value\":\"0\"},"
					+ "{\"name\":\"EMAIL_SOLICITANTE\",\"value\":\""+aux+"\"},"
					+ "{\"name\":\"EMAIL_ASSUNTO\",\"value\":\""+assuntoEmail+"\"}"
					+ "]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
			Post_JSON(query_url,request2); //REQUISICAO POST
			
			BigDecimal idflow = pegaIdProcesso(id); //PEGA O IDINSTPRN CRIADO
			salvaIdNaTelaEmailFlow(id, idflow);
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL CRIAR UMA TAREFA NO FLOW "+e.getMessage());
		}
		
	}
	
	//1.3.1
	public void Post_JSON(String query_url,String request){
		
		try {
			
			URL url = new URL(query_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			
			OutputStream os = conn.getOutputStream();
			byte[] b = request.getBytes("UTF-8");
			os.write(b);
			os.flush();
			os.close();
			
			InputStream in = new BufferedInputStream(conn.getInputStream());
			byte[] res = new byte[2048];
			int i = 0;
			StringBuilder response = new StringBuilder();
			while ((i = in.read(res)) != -1) {
				response.append(new String(res, 0, i));
			}
			in.close();

			//System.out.println("Response= " + response.toString());
			resp = response.toString();
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - ERRO NA REQUISICAO POST DO FLOW "+e.getMessage());
		}
	}
	
	//1.3.2
	public BigDecimal pegaVersao() throws Exception {
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(VERSAO) AS VERSAO FROM TWFPRN WHERE CODPRN="+programa);
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("VERSAO");
		}
		return count;
	}
	
	//1.3.3
	private BigDecimal pegaIdProcesso(BigDecimal id) throws Exception {
			BigDecimal count = null;

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MAX(IDINSTPRN) AS IDINSTPRN FROM TWFIPRN WHERE CODPRN="+programa+" AND DHCONCLUSAO IS NULL AND CODUSUINC="+codusu);
			contagem = nativeSql.executeQuery();

			while (contagem.next()) {
				count = contagem.getBigDecimal("IDINSTPRN");
			}
			idflow = count;
			return count;
		}
	
	//1.3.4
	private void salvaIdNaTelaEmailFlow(BigDecimal id, BigDecimal idflow) throws Exception {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_EMAILFLOW","this.ID=?", new Object[] { id}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;
			
			if(VO!=null) {
				VO.setProperty("FLOW", idflow);
			}

			itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O ID FLOW NA TELA EMAILS FLOW "+e.getMessage());
		}
	}
	
		
	// SALVA DADOS E ANEXO NO FLOW
	
	//1.4
	private void insereDadosNoFluxo(BigDecimal id) throws Exception {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EMAILSFLOW");
			DynamicVO VO = (DynamicVO) NPVO;
			
			String emailSolicitanteOriginal = emailSolicitante;
			String aux = emailSolicitanteOriginal.substring(emailSolicitanteOriginal.indexOf("<")+1,emailSolicitanteOriginal.lastIndexOf(">"));
			
			VO.setProperty("IDINSTPRN", idflow);
			VO.setProperty("IDINSTTAR", new BigDecimal(0));
			VO.setProperty("CODREGISTRO", new BigDecimal(1));
			VO.setProperty("IDTAREFA", tarefaDeContratos);
			VO.setProperty("EMAIL_ID", id);
			VO.setProperty("EMAIL_SOLICITANTE", aux);
			VO.setProperty("EMAIL_ASSUNTO", assuntoEmail);
			VO.setProperty("EMAIL_DATA", dtEmail);
			VO.setProperty("EMAIL_CONTEUDO", conteudo);
			
			dwfFacade.createEntity("AD_EMAILSFLOW", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL INSERIR DADOS NO FLUXO! "+e.getMessage());
		}

	}
	
	//1.5
	private void insertAnexoNoFluxoFLow(BigDecimal id) throws Exception {
	  
	  try {
	  
	  EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
	  
	  Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("InstanciaVariavel","this.IDINSTPRN=? AND this.NOME=? ", new Object[] { idflow,campoQueRecebeAnexoComercial })); 
	  for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) 
	  { PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next(); 
	  EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class); 
	  DynamicVO VO = (DynamicVO) NVO;
	  
	  if(VO!=null) {
	  
	  BigDecimal idProcessoNaTabela = VO.asBigDecimal("IDINSTPRN");
	  
	  if(idProcessoNaTabela.intValue()==idflow.intValue()) {
	  VO.setProperty("TEXTO", nomeAnexo); 
	  VO.setProperty("TEXTOLONGO","{\"name\":\""+nomeAnexo+"\",\"size\":0,\"type\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"path\":\"Repo://Sistema/workflow/formularios/arquivos/"+nomeArquivo+"\",\"lastModifiedDate\":\""+new Timestamp(System.currentTimeMillis()).toString()+"\",\"codUsu\":"+codusu+"}");
	  
	  }else { 
		  System.out.println("### FLOW ### - ID DO PROCESSO: "+idflow+" DIFERENTE DO ID DA TELA: "+idProcessoNaTabela); 
		  }
	  }
	  
	  //VO.setProperty("IDINSTPRN", idprocesso); //String longo ="{\"name\":\""+assuntoEmail+".xls\",\"size\":0,\"type\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"path\":\"Repo://Sistema/workflow/formularios/arquivos/"+nomeArquivo+"\",\"lastModifiedDate\":\""+new Timestamp(System.currentTimeMillis()).toString()+"\",\"codUsu\":"+codusu+"}";
	  //System.out.println("===> TEXTO LONGO:\n"+longo);
	  
	  itemEntity.setValueObject((EntityVO) VO); }
	  
	  } catch (Exception e) {
	  System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O ANEXO NO FLOW! "
	  +e.getMessage()); }
	  
	  }
	 
	
	// ENVIA E-MAIL CONFIRMANDO A CRIACAO DO FLUXO
	
	//1.6
	private void enviarEmail(String email, BigDecimal idFlow) throws Exception {
		try {
			
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "A sua solicitação referente ao e-mail \""+assuntoEmail+"\" enviado na data ("+dtEmail+"). "
					+ "<br/><br/>Gerou o fluxo flow número: <b>"+idFlow+"</b>."
					+ "<br/><br/>O processo está em analise por parte do setor de contratos."
					+ "<br/><br/><b>Este é um e-mail automático por gentileza não responder !</b>"
					+ "<br/><br/>Atencionamente,"
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
			VO.setProperty("ASSUNTO", new String("FLOW - "+idFlow));
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));	
			VO.setProperty("CODSMTP", new BigDecimal(1));
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL ENVIAR E-MAIL FLOW "+e.getMessage());
		}
		
	}
	
	//1.6.1
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
	
	//1.7
	private void verificaEmCopia() throws Exception {
		while(token.hasMoreTokens()) {
			String local = token.nextToken();
			
			enviarEmailEmCopia(local);
		}
	}
	
	//1.7.1
	private void enviarEmailEmCopia(String email) throws Exception {
		try {
			
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "A solicitação referente ao e-mail \""+assuntoEmail+"\" enviado na data ("+dtEmail+") por \""+emailSolicitante+"\". "
					+ "<br/><br/>Gerou o fluxo flow número: <b>"+idflow+"</b>."
					+ "<br/><br/>O processo está em analise por parte do setor de contratos."
					+ "<br/><br/><b>Este é um e-mail automático por gentileza não responder !</b>"
					+ "<br/><br/>Atencionamente,"
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
			VO.setProperty("ASSUNTO", new String("FLOW - "+idflow));
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));	
			VO.setProperty("CODSMTP", new BigDecimal(1));
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");

			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL ENVIAR E-MAIL EM COPIA DO FLOW "+e.getMessage());
		}
		
	}
		
	//1.8
	private void enviarEmailValidacao(String email, BigDecimal idFlow) {
		try {
			
			String mensagem = new String();
			
			mensagem = "Prezado,<br/>"
					+"<br/> <b>Validação de E-mail:</b>"
					+"<br/> <b>Id Flow: </b>"+idFlow
					+"<br/> <b>Solicitante: </b>"+emailSolicitante
					+"<br/> <b>Data: </b>"+dtEmail
					+"<br/> <b>Assunto: </b>"+assuntoEmail
					+"<br/> <b>Conteudo: </b>\""+conteudo+"\""
					+ "<br/><br/>Atencionamente,"
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
			VO.setProperty("ASSUNTO", new String("FLOW - "+idFlow));
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL ENVIAR E-MAIL FLOW "+e.getMessage());
		}
	}
	
	//SALVA NA TELA GERENCIAL
	
	//1.9
	private void salvaFluxoNaTelaGerencial() {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_GERENCIAINST");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("IDFLOW", idflow);
			
			dwfFacade.createEntity("AD_GERENCIAINST", (EntityVO) VO);
	
		} catch (Exception e) {
			System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR NA TELA DE GERENCIA "+e.getMessage());
		}
	}
	
	//MÉTODOS COMPLEMENTARES
	
	//CRIA NOME DO ARQUIVO PARA O REPOSITÓRIO DE ARQUIVOS
	private String buildChaveArquivo() {
		String a = new Timestamp(System.currentTimeMillis()).toString();
		String b = a.replaceAll("[^a-zZ-Z1-9]","");
		String c = b+cont;
		return c;
	}
	
	//CONVERSAO DE INPUT STREAM PARA BYTE
	public byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}
	
	//PEGA APENAS O JSESSION ID
	public String getJssesionId(String response) {
		String jsessionid = null;
		
		Pattern p = Pattern.compile("<jsessionid>(\\S+)</jsessionid>");
		Matcher m = p.matcher(response);
		if (m.find()) {
			jsessionid = m.group(1);
		}
		
		return jsessionid;
	}
	
	
	//MÉTODOS ANTIGOS
	/*
	 * private BigDecimal pegaIdProcesso(BigDecimal id) throws Exception {
	 * BigDecimal count = null;
	 * 
	 * JdbcWrapper jdbcWrapper = null; EntityFacade dwfEntityFacade =
	 * EntityFacadeFactory.getDWFFacade(); jdbcWrapper =
	 * dwfEntityFacade.getJdbcWrapper();
	 * 
	 * ResultSet contagem; NativeSql nativeSql = new NativeSql(jdbcWrapper);
	 * nativeSql.resetSqlBuf();
	 * nativeSql.appendSql("SELECT IDINSTPRN FROM TWFIVAR WHERE NOME='"
	 * +campoIdDoEmail+"' AND TEXTO='"+id.toString()
	 * +"' AND IDINSTPRN IN (SELECT IDINSTPRN FROM TWFIPRN WHERE DHCONCLUSAO IS NULL AND CODUSUINC="
	 * +codusu+")"); contagem = nativeSql.executeQuery();
	 * 
	 * while (contagem.next()) { count = contagem.getBigDecimal("IDINSTPRN"); }
	 * idflow = count; return count; }
	 */
	
	/*
	 * private void insertAnexoNoFluxoFLow(BigDecimal id) throws Exception {
	 * 
	 * try {
	 * 
	 * EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
	 * 
	 * Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new
	 * FinderWrapper("InstanciaVariavel","this.IDINSTPRN=? AND this.NOME=? ", new
	 * Object[] { idflow,campoQueRecebeAnexoComercial })); for (Iterator<?> Iterator
	 * = parceiro.iterator(); Iterator.hasNext();) { PersistentLocalEntity
	 * itemEntity = (PersistentLocalEntity) Iterator.next(); EntityVO NVO =
	 * (EntityVO) ((DynamicVO)
	 * itemEntity.getValueObject()).wrapInterface(DynamicVO.class); DynamicVO VO =
	 * (DynamicVO) NVO;
	 * 
	 * if(VO!=null) {
	 * 
	 * BigDecimal idProcessoNaTabela = VO.asBigDecimal("IDINSTPRN");
	 * 
	 * if(idProcessoNaTabela.intValue()==idflow.intValue()) {
	 * VO.setProperty("TEXTO", nomeAnexo); VO.setProperty("TEXTOLONGO",
	 * "{\"name\":\""+nomeAnexo+
	 * "\",\"size\":0,\"type\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"path\":\"Repo://Sistema/workflow/formularios/arquivos/"
	 * +nomeArquivo+"\",\"lastModifiedDate\":\""+new
	 * Timestamp(System.currentTimeMillis()).toString()+"\",\"codUsu\":"+codusu+"}")
	 * ;
	 * 
	 * }else { System.out.println("### FLOW ### - ID DO PROCESSO: "
	 * +idflow+" DIFERENTE DO ID DA TELA: "+idProcessoNaTabela); }
	 * 
	 * }
	 * 
	 * //VO.setProperty("IDINSTPRN", idprocesso); //String longo =
	 * "{\"name\":\""+assuntoEmail+
	 * ".xls\",\"size\":0,\"type\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"path\":\"Repo://Sistema/workflow/formularios/arquivos/"
	 * +nomeArquivo+"\",\"lastModifiedDate\":\""+new
	 * Timestamp(System.currentTimeMillis()).toString()+"\",\"codUsu\":"+codusu+"}";
	 * //System.out.println("===> TEXTO LONGO:\n"+longo);
	 * 
	 * itemEntity.setValueObject((EntityVO) VO); }
	 * 
	 * } catch (Exception e) {
	 * System.out.println("### FLOW ### - NAO FOI POSSIVEL SALVAR O ANEXO NO FLOW! "
	 * +e.getMessage()); }
	 * 
	 * }
	 */
	
}

