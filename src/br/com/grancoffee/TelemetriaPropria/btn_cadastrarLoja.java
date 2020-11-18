package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.DecimalFormat;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_cadastrarLoja implements AcaoRotinaJava {

	String erro = "";

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) {
		String nome = (String) arg0.getParam("NOME");
		String endereco = (String) arg0.getParam("ENDERECO");
		String contrato =  (String) arg0.getParam("CONTRATO");

		String totem = totem();
		cadastrarAdPatrimonio(totem,nome,new BigDecimal(contrato));
		cadastrarTelaInstalacoes(totem,new BigDecimal(contrato),endereco);

		if (erro != "") {
			arg0.setMensagemRetorno("Erro \n" + erro);
		} else {
			arg0.setMensagemRetorno("Loja Cadastrada! - <b>" + totem + "</b>");
		}
	}

	private String totem() {
		DecimalFormat df = new DecimalFormat("0000");
		String ultimoTotem = getUltimoTotem();
		String novoTotem = "CORNER" + df.format(Integer.decode(StringUtils.substr(ultimoTotem, 7, 4)) + 1);
		return novoTotem;
	}

	private String getUltimoTotem() {
		String bem = "CORNER0001";

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MAX(CODBEM) AS BEM FROM AD_PATRIMONIO WHERE CODBEM LIKE '%CORNER%'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				bem = contagem.getString("BEM");
			}
		} catch (Exception e) {
			erro = "Não foi possível determinar o último Corner!" + e.getMessage();
		}

		return bem;
	}

	private void cadastrarAdPatrimonio(String totem, String descricao,BigDecimal contrato) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("PATRIMONIO");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", totem);
			VO.setProperty("DESCBEM", descricao.trim().toUpperCase().replaceAll("\s+", " "));
			VO.setProperty("AD_INTENVIA", "S");
			VO.setProperty("ENVIADO_GCW", "N");
			VO.setProperty("CODPROD", new BigDecimal(514410));
			
			if(contrato!=null) {
				VO.setProperty("NUMCONTRATO", contrato);
				VO.setProperty("CODPARC", getNumcontrato(contrato).asBigDecimal("CODPARC"));
			}

			dwfFacade.createEntity("PATRIMONIO", (EntityVO) VO);
		} catch (Exception e) {
			erro = "Não foi possível cadastrar a loja!" + e.getMessage();
		}
	}
	
	private void cadastrarTelaInstalacoes(String totem,BigDecimal contrato,String endereco) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCInstalacao");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ABASTECIMENTO", "S");
			VO.setProperty("AD_CORNER", "S");
			VO.setProperty("AD_IDPLANTA", new BigDecimal(endereco));
			VO.setProperty("AD_NUMCONTRATO", contrato);
			VO.setProperty("CODBEM", totem);
			VO.setProperty("PLANOGRAMAPENDENTE", "N");
			VO.setProperty("TOTEM", "S");
			
			dwfFacade.createEntity("GCInstalacao", (EntityVO) VO);
			
		} catch (Exception e) {
			erro = "Não foi possível cadastrar na tela Instalações!" + e.getMessage();
		}
	}
	
	private DynamicVO getNumcontrato(BigDecimal contrato) throws Exception {
		DynamicVO VO = null;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("Contrato");
			VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		} catch (Exception e) {
			erro = "Não foi possível determinar qual o contrato!" + e.getMessage();
		}
		
		return VO;
	}
	
	
}
