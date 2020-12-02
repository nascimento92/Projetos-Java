package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_EnviarPlanograma implements AcaoRotinaJava {

	BigDecimal planogramaAtual = BigDecimal.ZERO;
	BigDecimal novos = BigDecimal.ZERO;
	BigDecimal aumentoValor = BigDecimal.ZERO;
	BigDecimal reducaoValor = BigDecimal.ZERO;
	BigDecimal aumentopar = BigDecimal.ZERO;
	BigDecimal reducaopar = BigDecimal.ZERO;

	String erro = "";

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String patrimonio = (String) linhas[0].getCampo("CODBEM");

		verificaPossibilidades(patrimonio);
		verificaSeJaExiteUmPlanogramaAtual(patrimonio);
		
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String totem = gcInstalacao.asString("TOTEM");
		
		if(totem==null) {
			totem="N";
		}
		
		if(this.planogramaAtual.intValue()==0) { //enviar planograma
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"S");
			chamaPentaho();
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"N");
		}
		
		if (this.novos.intValue() > 0 && this.planogramaAtual.intValue() > 0) {
			throw new Error("<br/><br/><b>Existem produtos novos neste planograma, gerar um reabastecimento!</b><br/><br/>");
		}
		
		if (this.aumentopar.intValue() > 0 && this.planogramaAtual.intValue() > 0) {
			throw new Error("<br/><br/><b>Existem aumentos de nivel par, gerar um reabastecimento!</b><br/><br/>");
		}
		
		if("N".equals(gcInstalacao.asString("TOTEM")) && (aumentoValor.intValue()>0 || reducaoValor.intValue()>0)) {
			//gerar uma visita simples para alterar o preço
		}
		
		if("S".equals(gcInstalacao.asString("TOTEM")) && (aumentoValor.intValue()>0 || reducaoValor.intValue()>0)) {
			//efetivar a alteração do preço
		}

		if (this.erro != "") {
			arg0.setMensagemRetorno(this.erro);
		} else {
			arg0.setMensagemRetorno("Planograma enviado!");
		}
	}

	private void verificaPossibilidades(String patrimonio) {
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT\r\n"
					+ "(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='NOVO' AND CODBEM=T.CODBEM) AS NOVO_PRODUTO,\r\n"
					+ "(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='AUMENTO' AND CODBEM=T.CODBEM) AS AUMENTO_VALOR,\r\n"
					+ "(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='REDUCAO' AND CODBEM=T.CODBEM) AS REDUCAO_VALOR,\r\n"
					+ "(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='AUMENTO' AND CODBEM=T.CODBEM) AS AUMENTO_PAR,\r\n"
					+ "(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='REDUCAO' AND CODBEM=T.CODBEM) AS REDUCAO_PAR\r\n"
					+ "FROM GC_INSTALACAO T\r\n" + "WHERE T.CODBEM='" + patrimonio + "'");
			ResultSet contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				this.novos = contagem.getBigDecimal("NOVO_PRODUTO");
				this.aumentoValor = contagem.getBigDecimal("AUMENTO_VALOR");
				this.reducaoValor = contagem.getBigDecimal("REDUCAO_VALOR");
				this.aumentopar = contagem.getBigDecimal("AUMENTO_PAR");
				this.reducaopar = contagem.getBigDecimal("REDUCAO_PAR");
			}
		} catch (Exception e) {
			String erro = "Nao foi possivel encontrar os dados do patrimonio! \n" + e.getMessage() + "\n"
					+ e.getCause();
			salvarException(erro);
		}
	}

	private void verificaSeJaExiteUmPlanogramaAtual(String patrimonio) {
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM AD_PLANOGRAMAATUAL WHERE CODBEM='" + patrimonio + "'");
			ResultSet contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				this.planogramaAtual = contagem.getBigDecimal("QTD");
			}
		} catch (Exception e) {
			String erro = "Nao foi possivel validar se existe planograma atual! \n" + e.getMessage() + "\n"+ e.getCause();
			salvarException(erro);
		}
	}
	
	private DynamicVO getGcInstalacao(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private void marcarEnviarPlanogramaNaTelaInstalacao(String patrimonio, String tipo) {
		try {
			JapeWrapper parceiroDAO = JapeFactory.dao("GCInstalacao");
			parceiroDAO.prepareToUpdateByPK(patrimonio)
				.set("AD_ENVIARPLANOGRAMA",tipo)
				.update();

		} catch (Exception e) {
			String erro = "[marcarEnviarPlanogramaNaTelaInstalacao] Nao foi possivel gravar o campo enviar planograma! \n" + e.getMessage() + "\n"+ e.getCause();
			salvarException(erro);
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Transformations/";
			String objName = "TF - GSN009 - Enviar Planograma";
			String objName2 = "TF - GSN009 - Enviar Planograma Loja Uppay";

			si.runTrans(path, objName);
			si.runTrans(path, objName2);

		} catch (Exception e) {
			erro = "Não foi possível chamar a Rotina Pentaho!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}

	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_EnviarPlanograma");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
