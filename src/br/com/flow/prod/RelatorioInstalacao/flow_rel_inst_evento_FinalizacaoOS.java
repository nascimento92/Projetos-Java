package br.com.flow.prod.RelatorioInstalacao;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_rel_inst_evento_FinalizacaoOS implements EventoProgramavelJava {
	/**
	 * Evento que fica verificando as Ordens de serviço, quando uma for finalizada e esta foi gerada pelo flow, irá encerrar a tarefa do flow;
	 */
	private String campoNome = "SISTEMA_NROS";
	private BigDecimal idProcesso = null;
	
	private String http = "localhost:8180";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String resp = null;
	private String jsessionID = null;
	
	private String processId = "8";
	//private String idelemento = "UserTask_1ehlgya";
	private String idelemento = "UserTask_1y3g2tm";
	private BigDecimal idTarefa = null;
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		try {
			start(arg0);
		} catch (Exception e) {
			System.out.println("NAO FOI POSSIVEL VERIFICAR O FLUXO FLOW! "+e.getMessage());
		}
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dtFechamento = VO.asTimestamp("DTFECHAMENTO");
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		String situacao = VO.asString("SITUACAO");
		
		if(dtFechamento!=null && "F".equals(situacao)) {
			if(validaSeEstaNoFluxoDoFlow(numos)) {
				finalizaTarefa();
				System.out.println("\n\nA OS: "+numos+" ESTA NO FLUXO: "+idProcesso+" JSESSION ID: "+jsessionID+" ID TAREFA: "+idTarefa+"\n\n");
			}
			
			BigDecimal usuFechamento = VO.asBigDecimal("CODUSUFECH");

			insereOhEncerramentoNoLog(numos,usuFechamento);
		}
	}
	
	private boolean validaSeEstaNoFluxoDoFlow(BigDecimal numos) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("InstanciaVariavel");
		DynamicVO VO = DAO.findOne("NOME=? AND TEXTO=?",new Object[] { this.campoNome, numos.toString() });
		
		if(VO!=null) {
			valida = true;
			idProcesso = VO.asBigDecimal("IDINSTPRN");
		}

		return valida;
	}
	
	private void finalizaTarefa() throws Exception {
		
		String url = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.login";
		String request1 = "<serviceRequest serviceName=\"MobileLoginSP.login\">\r\n" +
				  " <requestBody>\r\n" + " <NOMUSU>"+nomeusu+"</NOMUSU>\r\n" +
				  " <INTERNO>"+senha+"</INTERNO>\r\n" + " </requestBody>\r\n" +
				  " </serviceRequest>";
		Post_JSON(url,request1);
		jsessionID = getJssesionId(resp);
		
		try {
			getIdTarefa();
		} catch (Exception e) {
			System.out.println("Nao foi possivel obter o id da tarefa "+e.getMessage());
		}
		
		System.out.println("\nJSESSION ID: "+jsessionID+"\nIDPROJETO: "+processId+"\nIDPROCESSO: "+idProcesso+"\nIDTAREFA: "+idTarefa+"\nIDELEMENTO: "+idelemento);
		
		String query_url = "http://"+http+"/workflow/service.sbr?serviceName=ListaTarefaSP.finishTask&application=ListaTarefa&mgeSession="+jsessionID;
		String request = "{\"serviceName\":\"ListaTarefaSP.finishTask\",\"requestBody\":{\"param\":{\"processId\":"+processId+",\"processInstanceId\":\""+idProcesso+"\",\"taskInstanceId\":\""+idTarefa+"\",\"taskIdElemento\":\""+idelemento+"\"}}}";
		
		System.out.println("============> URL "+query_url+"\nREQUEST: "+request+"\nJSESSION ID: "+jsessionID);
		
		Post_JSON(query_url,request);
	}
	
	private void Post_JSON(String query_url,String request){
		
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
			System.out.println("ERRRO !"+e.getMessage());
		}
	}
	
	private String getJssesionId(String response) {
		String jsessionid = null;
		
		Pattern p = Pattern.compile("<jsessionid>(\\S+)</jsessionid>");
		Matcher m = p.matcher(response);
		if (m.find()) {
			jsessionid = m.group(1);
		}
		
		return jsessionid;
	}
	
	private void getIdTarefa() throws Exception {
		JapeWrapper DAO = JapeFactory.dao("InstanciaTarefa");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? AND IDELEMENTO=?",new Object[] { this.idProcesso, this.idelemento });
		
		if(VO!=null) {
			idTarefa = null;
			idTarefa = VO.asBigDecimal("IDINSTTAR");
		}

	}
	
	//salva no LOG
	private BigDecimal getUltimoIdDoLog() throws Exception {
		int count = 0;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(ID)+1 AS CODFILA FROM AD_LOG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}

		BigDecimal ultimoCodigo = new BigDecimal(count);

		return ultimoCodigo;
	}
	
	private void insereOhEncerramentoNoLog(BigDecimal numos, BigDecimal usuarioFechamento) {
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ID", getUltimoIdDoLog());
			VO.setProperty("TABELA", "TCSOSE");
			VO.setProperty("CAMPO", "SITUACAO");
			VO.setProperty("VLROLD", "PENDENTE");
			VO.setProperty("VLRNEW", "FECHADA");
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("PKTABELA", numos.toString());
			VO.setProperty("CODUSU", usuarioFechamento);

			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("NAO FOI POSSIVEL SALVAR NO LOG O ENCERRAMENTO DA OS");
		}	
	}
}
