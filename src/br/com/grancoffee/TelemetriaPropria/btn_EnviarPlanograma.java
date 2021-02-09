package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_EnviarPlanograma implements AcaoRotinaJava {

	BigDecimal planogramaAtual = BigDecimal.ZERO;
	BigDecimal novos = BigDecimal.ZERO;
	BigDecimal aumentoValor = BigDecimal.ZERO;
	BigDecimal reducaoValor = BigDecimal.ZERO;
	BigDecimal aumentopar = BigDecimal.ZERO;
	BigDecimal reducaopar = BigDecimal.ZERO;
	BigDecimal retirar = BigDecimal.ZERO;

	String erro = "";

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String patrimonio = (String) linhas[0].getCampo("CODBEM");
		
		marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"N");
		verificaPossibilidades(patrimonio);
		verificaSeJaExiteUmPlanogramaAtual(patrimonio);
		
		DynamicVO gcInstalacao = getGcInstalacao(patrimonio);
		String totem = gcInstalacao.asString("TOTEM");
		
		if(totem==null) {
			totem="N";
		}
		
		if(this.novos.intValue()>0 && this.planogramaAtual.intValue()==0) {
			//Cadastro Novo, pode ser enviado a loja
			chamaPentahoCriarLoja();
		}
		
		if ("N".equals(gcInstalacao.asString("TOTEM")) && this.novos.intValue() > 0 && this.planogramaAtual.intValue() > 0) {
			throw new Error("<br/><br/><b>Existem produtos novos neste planograma, gerar um reabastecimento!</b><br/><br/>");
		}
		
		if ("N".equals(gcInstalacao.asString("TOTEM")) && this.aumentopar.intValue() > 0 && this.planogramaAtual.intValue() > 0) {
			throw new Error("<br/><br/><b>Existem aumentos de nivel par, gerar um reabastecimento!</b><br/><br/>");
		}
		
		if("N".equals(gcInstalacao.asString("TOTEM")) && (aumentoValor.intValue()>0 || reducaoValor.intValue()>0)) {
			//gerar uma visita simples para alterar o preço
		}
		
		if("S".equals(gcInstalacao.asString("TOTEM")) && (aumentoValor.intValue()>0 || reducaoValor.intValue()>0)) {
			//efetivar a alteração do preço
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"S");
			chamaPentahoEnviarPlanograma();
			getPrecos(patrimonio);
		}
		
		if("S".equals(gcInstalacao.asString("TOTEM"))) { //temp
			marcarEnviarPlanogramaNaTelaInstalacao(patrimonio,"S");
			chamaPentahoEnviarPlanograma();
			getPrecos(patrimonio);
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
					+ "(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='REDUÇÃO' AND CODBEM=T.CODBEM) AS REDUCAO_VALOR,\r\n"
					+ "(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='AUMENTO' AND CODBEM=T.CODBEM) AS AUMENTO_PAR,\r\n"
					+ "(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='REDUÇÃO' AND CODBEM=T.CODBEM) AS REDUCAO_PAR,\r\n"
					+ "(SELECT RETIRAR FROM GC_TP_TROCADEGRADE_GERAL WHERE CODBEM=T.CODBEM AND ROWNUM=1) AS RETIRAR\r\n"
					+ "FROM GC_INSTALACAO T\r\n" + "WHERE T.CODBEM='" + patrimonio + "'");
			ResultSet contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				this.novos = contagem.getBigDecimal("NOVO_PRODUTO");
				this.aumentoValor = contagem.getBigDecimal("AUMENTO_VALOR");
				this.reducaoValor = contagem.getBigDecimal("REDUCAO_VALOR");
				this.aumentopar = contagem.getBigDecimal("AUMENTO_PAR");
				this.reducaopar = contagem.getBigDecimal("REDUCAO_PAR");
				this.retirar = contagem.getBigDecimal("RETIRAR");
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
	
	private void chamaPentahoEnviarPlanograma() {

		try {

			String site = "http://10.100.41.4:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Jobs/";
			String objName = "JOB - GSN009 - Enviar Planograma";
			
			si.runJob(path, objName);

		} catch (Exception e) {
			erro = "Não foi possível chamar a Rotina Pentaho Enviar planograma!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}
	
	private void chamaPentahoCriarLoja() {
		
		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Jobs/";
			String objName = "JOB - GSN009 - CRIAR LOJA";
			
			si.runJob(path, objName);

		} catch (Exception e) {
			erro = "Não foi possível chamar a Rotina Pentaho criar Loja!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}
	
	private void getPrecos(String patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma","this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = (String) DynamicVO.getProperty("TECLA");
			BigDecimal produto = (BigDecimal) DynamicVO.getProperty("CODPROD");
			BigDecimal vlrpar = (BigDecimal) DynamicVO.getProperty("VLRPAR");
			BigDecimal vlrfun = (BigDecimal) DynamicVO.getProperty("VLRFUN");
			
			setPrecos(patrimonio,tecla,produto,vlrpar,vlrfun);
			}
			
		} catch (Exception e) {
			erro = "Não foi possível obter o preço!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}
	
	private void setPrecos(String patrimonio, String tecla, BigDecimal produto, BigDecimal vlrpar, BigDecimal vlrfun) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAATUAL",
					"this.CODBEM=? AND this.TECLA=? AND this.CODPROD=? ", new Object[] { patrimonio, tecla, produto }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("VLRPAR", vlrpar);
				VO.setProperty("VLRFUN", vlrfun);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			erro = "Não foi possível atualizar o preço!" + e.getMessage() + "\n" + e.getCause();
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
