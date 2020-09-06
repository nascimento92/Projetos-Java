package br.com.flow.RelatorioInstalacao;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoIniciaTarefaFlow implements EventoProgramavelJava {
	
	private String resp = null;
	private String http = "192.168.0.80:8280";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String jsessionID = null;
	private String programa = "5";
	private String version = null;

	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {

		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		//start(arg0);
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		//start(arg0);
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal id = VO.asBigDecimal("ID");
		
		if(id==null) {
			id = new BigDecimal(1);
		}
		
		String url = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.login";
		String request1 = "<serviceRequest serviceName=\"MobileLoginSP.login\">\r\n" +
				  " <requestBody>\r\n" + " <NOMUSU>"+nomeusu+"</NOMUSU>\r\n" +
				  " <INTERNO>"+senha+"</INTERNO>\r\n" + " </requestBody>\r\n" +
				  " </serviceRequest>";
		
		Post_JSON(url,request1);
		jsessionID = getJssesionId(resp);
		version = pegaVersao().toString();
		
		
		System.out.println(
				"JSESSION ID: "+jsessionID+
				"\nULTIMA VERSAO: "+version+
				"\nID: "+id);
		
		String query_url = "http://"+http+"/workflow/service.sbr?serviceName=ListaTarefaSP.startProcess&counter=79&application=ListaTarefa&mgeSession="+jsessionID;
		String request = "{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":5,\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_5_VERSION_+"+version+"\",\"parentEntity\":\"-99999999\",\"records\":[{\"record\":[{\"name\":\"EMAIL\",\"value\":\"+"+id.toString()+"\"}]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
		//String request = "{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":5,\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_5_VERSION_53\",\"parentEntity\":\"-99999999\",\"records\":[{\"record\":[{\"name\":\"EMAIL\",\"value\":\"2\"}]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
		
		Post_JSON(query_url,request);
		
		
	}
	
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
			System.out.println("ERRRO !"+e.getMessage());
		}
	}
	
	public String getJssesionId(String response) {
		String jsessionid = null;
		
		Pattern p = Pattern.compile("<jsessionid>(\\S+)</jsessionid>");
		Matcher m = p.matcher(response);
		if (m.find()) {
			jsessionid = m.group(1);
		}
		
		return jsessionid;
	}
	
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
}
