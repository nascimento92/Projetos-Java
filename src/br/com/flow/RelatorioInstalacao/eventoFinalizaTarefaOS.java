package br.com.flow.RelatorioInstalacao;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class eventoFinalizaTarefaOS implements EventoProgramavelJava {
	
	private String campoNome = "SISTEMA_NROS";
	private BigDecimal idProcesso = null;
	
	private String http = "192.168.0.80:8280";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String resp = null;
	private String jsessionID = null;
	
	private String processId = "7";
	//private String idelemento = "UserTask_1u45pup";
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
		
		if(dtFechamento!=null) {
			if(validaSeEstaNoFluxoDoFlow(numos)) {
				finalizaTarefa();
				System.out.println("A OS: "+numos+" ESTA NO FLUXO: "+idProcesso+" JSESSION ID: "+jsessionID+" ID TAREFA: "+idTarefa);
			}
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
		
		String query_url = "http://"+http+"/workflow/service.sbr?serviceName=ListaTarefaSP.finishTask&application=ListaTarefa&mgeSession="+jsessionID;
		String request = "{\"serviceName\":\"ListaTarefaSP.finishTask\",\"requestBody\":{\"param\":{\"processId\":"+processId+",\"processInstanceId\":\""+idProcesso+"\",\"taskInstanceId\":\""+idTarefa+"\",\"taskIdElemento\":\""+idelemento+"\"}}}";
		
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

}
