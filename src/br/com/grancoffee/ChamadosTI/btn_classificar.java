package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_classificar implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		if(linhas.length==1) {
			start(linhas,arg0);
		}			
	}
	
	public void start(Registro[] linhas,ContextoAcao arg0) throws Exception {			
		
		//String email = (String) linhas[0].getCampo("EMAIL");
		//BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		//String tipo = validaTipo((String) arg0.getParam("TIPOSOLICITACAO"));
		//String prioridade = validaPrioridade((String) arg0.getParam("PRIORIDADE"));
		//String nivel = validaNivel((String) arg0.getParam("NIVEL"));
		//String classificacao = validaClassificacao((String) arg0.getParam("CLASSIFICACAO"));
		Timestamp dataFinal = (Timestamp) linhas[0].getCampo("DTFECHAMENTO");
		//String descricaoAbreviada = StringUtils.substr(linhas[0].getCampo("DESCRICAO").toString(), 0, 100);
		//String area = validaArea((String) arg0.getParam("AREA"));
		BigDecimal idFlow = (BigDecimal) linhas[0].getCampo("IDFLOW");
		
		if(idFlow!=null) {
			arg0.mostraErro("<br/><br/>Chamado aberto pelo Flow \"Chamados / Projetos\" deverá ser classificado pela tela <b>Lista de Tarefas</b>!<br/><br/>");
		}
		
		if(dataFinal!=null) {
			arg0.mostraErro("Chamado encerrado, não pode ser classificado!");
		}else {
			//enviarEmail(numos,email,tipo,classificacao,prioridade,nivel,descricaoAbreviada,area);
			setDados(linhas,arg0);
		}
	}
	
	public void setDados(Registro[] linhas,ContextoAcao arg0) throws Exception {
		linhas[0].setCampo("TIPO", arg0.getParam("TIPOSOLICITACAO"));
		linhas[0].setCampo("CLASSIFICACAO", arg0.getParam("CLASSIFICACAO"));
		linhas[0].setCampo("PRIORIDADE", arg0.getParam("PRIORIDADE"));
		linhas[0].setCampo("NIVEL", arg0.getParam("NIVEL"));
		linhas[0].setCampo("ATENDENTE", null);
		linhas[0].setCampo("DTPREVISTA", null);
		linhas[0].setCampo("AREA", arg0.getParam("AREA"));
		linhas[0].setCampo("RECORRENTE", arg0.getParam("RECORRENTE"));
	}
	
	/*
	private void enviarEmail(BigDecimal numos, String email, String tipo, String classificacao, String prioridade, String nivel, String descricao, String area) throws Exception {
		
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de número <b>"+numos+"</b> foi classificado."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/><b>Classificação:</b> "+classificacao
					+ "<br/><br/><b>Tipo:</b> "+tipo
					+ "<br/><br/><b>Prioridade:</b> "+prioridade
					+ "<br/><br/><b>Nivel Atendimento:</b> "+nivel
					+ "<br/><br/><b>Área de atuação:</b> "+area
					+ "<br/><br/><b>Esta é uma mensagem automática, por gentileza não respondê-la</b>"
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
			VO.setProperty("CODSMTP", new BigDecimal(1));
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.evento_criaOS] ## - NAO FOI POSSIVEL ENVIAR E-MAIL DE CONFIRMACAO DE ABERTURA DE CHAMADO"+e.getMessage());
			e.printStackTrace();
		}	
	}
	*/
	
	public String validaTipo(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Analise";
		break;
		
		case "2":tipoClassificado="Incidente";
		break;

		default:
			tipoClassificado="Analise";
			break;
		}
		
		return tipoClassificado;
	}
	
	public String validaArea(String area) {
		String tipoClassificado="";
		
		switch (area) {
		case "1":tipoClassificado="Infraestrutura";
		break;
		
		case "2":tipoClassificado="Sistemas";
		break;

		default:
			tipoClassificado="T.I";
			break;
		}
		
		return tipoClassificado;
	}
	
	public String validaPrioridade(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Baixa";
		break;
		
		case "2":tipoClassificado="Média";
		break;
		
		case "3":tipoClassificado="Alta";
		break;
		
		case "4":tipoClassificado="Urgente";
		break;

		default:
			tipoClassificado="Baixa";
			break;
		}
		
		return tipoClassificado;
	}
	 
	public String validaNivel(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Analise Nivel 1";
		break;
		
		case "2":tipoClassificado="Analise Nivel 2";
		break;
		
		case "3":tipoClassificado="Analise Nivel 3";
		break;
		
		case "4":tipoClassificado="Desenvolvimento";
		break;
		
		case "5":tipoClassificado="Gerencia";
		break;

		default:
			tipoClassificado="Analise Nivel 1";
			break;
		}
		
		return tipoClassificado;
	}
	
	public String validaClassificacao(String tipo) throws Exception {
		String descricao="";
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT DESCRICAO FROM AD_TIPOCHAMADOSTI WHERE CODTIPO ="+tipo);
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			descricao = contagem.getString("DESCRICAO");
		}
		
		return descricao;
	}
	
	/*
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
	*/
}
