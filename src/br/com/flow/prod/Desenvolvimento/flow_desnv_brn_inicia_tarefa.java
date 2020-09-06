package br.com.flow.prod.Desenvolvimento;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_desnv_brn_inicia_tarefa implements AcaoRotinaJava {
	
	private String http = "localhost:8180";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String resp = null;
	private String jsessionID = null;
	private String version = null;
	private String programa = "9";
	
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		if(linhas.length>1) {
			arg0.setMensagemRetorno("SELECIONE APENAS UM CHAMADO POR VEZ!");
			}
		else {
			start(linhas,arg0);
			}
	}
	
	//1.0
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		String nomeUsuario = null;
		String nomeCompleto = null;
		String prioridade = null;
		
		//dados da solicitação
		String ramal = (String) linhas[0].getCampo("CONTATO");
		String problema = (String) linhas[0].getCampo("DESCRICAO");
		String prioridadeSetor = (String) linhas[0].getCampo("SD_PRIORIDADE");
		BigDecimal ganhoDesenvolvimento = (BigDecimal) linhas[0].getCampo("SD_GANHO");
		String obsDesenvolvimento = (String) linhas[0].getCampo("SD_OBSERVACAO");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		BigDecimal codsolicitante = (BigDecimal) linhas[0].getCampo("CODUSU");
		
		//dados solicitante
		if(codsolicitante!=null) {
			nomeUsuario = tsiusu(codsolicitante).asString("NOMEUSU");
			nomeCompleto = tsiusu(codsolicitante).asString("NOMEUSUCPLT");
		}
		
		//organizando a prioridade
		if(prioridadeSetor!=null) {
			switch(prioridadeSetor) {
				case "1":
					prioridade="BAIXA";
					break;
				case "2":
					prioridade="MEDIA";
					break;
				case "3":
					prioridade="ALTA";
					break;
				default:
					prioridade="BAIXA";
			}
		}
		
		String Descricao =
				"CHAMADO: "+numos+
				"\n USUARIO SOLICITANTE: "+nomeUsuario+
				"\n NOME COMPLETO: "+nomeCompleto+
				"\n CONTATO: "+ramal+
				"\n\n PRIORIDADE PARA O SETOR: "+prioridade+
				"\n GANHO DESENVOLVIMENTO (HORAS): "+ganhoDesenvolvimento+
				"\n SOLICITAÇÃO: \"<i>"+problema+"\"</i>"+
				"\n OBSERVAÇÃO DESENVOLVIMENTO: \"<i>"+obsDesenvolvimento+"\"</i>"
				;
		
		//faz os requests
		requests(arg0,Descricao);
	}
	
	//1.1
	private void requests(ContextoAcao arg0, String descricao) throws Exception {
		//PEGA O JSESSION ID
		String url = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.login";
		String request1 = "<serviceRequest serviceName=\"MobileLoginSP.login\">\r\n" +
				  " <requestBody>\r\n" + " <NOMUSU>"+nomeusu+"</NOMUSU>\r\n" +
				  " <INTERNO>"+senha+"</INTERNO>\r\n" + " </requestBody>\r\n" +
				  " </serviceRequest>";
		
		Post_JSON(url,request1); //REQUISICAO POST
		jsessionID = getJssesionId(resp);
		version = pegaVersao().toString();
		
		arg0.setMensagemRetorno("JSESSION ID: "+jsessionID);
		
		//LOGOUT NA SESSAO
		String urlLogout = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.logout&mgeSession="+jsessionID;
		Post_JSON(urlLogout,"");
	}
	
	//MÉTODOS AUXILIARES
	
	//PEGA TSIUSU
	private DynamicVO tsiusu(BigDecimal codusu) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { codusu });
		return VO;
	}
	
	//REQUISICAO POST
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
			System.out.println("### FLOW DESENVOLVIMENTO ### - ERRO NA REQUISICAO POST DO FLOW "+e.getMessage());
		}
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
		
	//DESCOBRE A VERSAO DO FLOW
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
