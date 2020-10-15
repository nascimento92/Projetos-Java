package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_GerarMulta implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoTarefa arg0) throws Exception {
		Object idflow = arg0.getIdInstanceProcesso();
		criarMulta(idflow);
	}
	
	private void criarMulta(Object idflow) throws Exception {
		DynamicVO form = getForm(idflow);
		DynamicVO tcscon = getTCSCON(form.asBigDecimal("NUMCONTRATO"));
		
		BigDecimal tiponegociacao = tcscon.asBigDecimal("CODTIPVENDA");
		if(tiponegociacao==null) {
			tiponegociacao = new BigDecimal(100);
		}
		
		BigDecimal natureza = tcscon.asBigDecimal("CODNAT");
		if(natureza==null) {
			natureza = new BigDecimal(10200);
		}
		
		BigDecimal nunota = BigDecimal.ZERO;
		BigDecimal nuNotaModelo = new BigDecimal(133411899);
		
		try { //cria cabecalho
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;

			BigDecimal codTipOper = ModeloNPVO.asBigDecimal("CODTIPOPER");

			DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
			String tipoMovimento = topRVO.asString("TIPMOV");

			NotaProdVO.setProperty("CODTIPOPER", codTipOper);
			NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
			NotaProdVO.setProperty("CODPARC", form.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("NUMCONTRATO", form.asBigDecimal("NUMCONTRATO"));
			NotaProdVO.setProperty("CODTIPVENDA", tiponegociacao);
			NotaProdVO.setProperty("CODEMP", tcscon.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("CODNAT", natureza);
			NotaProdVO.setProperty("CODCENCUS", tcscon.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			NotaProdVO.setProperty("PENDENTE", "S");
			NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB"));
			NotaProdVO.setProperty("CODEMPNEGOC", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("TIPFRETE", "N");
			NotaProdVO.setProperty("CODVEND", new BigDecimal(1303));
			NotaProdVO.setProperty("OBSERVACAO", "REERENTE MULTA CONTRATUAL");

			dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO);
			nunota = NotaProdVO.asBigDecimal("NUNOTA");
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarMulta] ## - Nao foi possivel gerar cabecalho!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
		
		if(nunota.intValue()!=0) { //cria item
			
			insereNotaRetorno(nunota,idflow);
			
			try {
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemNota");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("CODPROD", new BigDecimal(377));
				VO.setProperty("NUNOTA", nunota);
				VO.setProperty("SEQUENCIA", new BigDecimal(1));
				VO.setProperty("CODEMP", tcscon.asBigDecimal("CODEMP"));
				VO.setProperty("QTDNEG", new BigDecimal(1));
				VO.setProperty("VLRUNIT", form.asBigDecimal("MULTA"));
				VO.setProperty("VLRTOT", form.asBigDecimal("MULTA"));
				VO.setProperty("ATUALESTOQUE", new BigDecimal(1));
				VO.setProperty("CODVOL", "UN");
				
				dwfFacade.createEntity("ItemNota", (EntityVO) VO);
				
			} catch (Exception e) {
				System.out.println("## [flow_cc_tarefaJava_GerarMulta] ## - Nao foi possivel salvar o valor da multa na nota!");
				e.getCause();
				e.getMessage();
				e.printStackTrace();
			}
		}
	}
	
	private void insereNotaRetorno(BigDecimal nunota, Object idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_NFCANCELAMENTO");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODREGISTRO", new BigDecimal(1));
			VO.setProperty("IDINSTPRN", idflow);
			VO.setProperty("IDINSTTAR", new BigDecimal(0));
			VO.setProperty("IDTAREFA", "UserTask_09szdie");
			VO.setProperty("NUNOTA", nunota);
			
			dwfFacade.createEntity("AD_NFCANCELAMENTO", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarNF] ## - Nao foi possivel salvar as NF de retorno!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private DynamicVO getForm(Object idflow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idflow });
		return VO;
	}
	
	private DynamicVO getTCSCON(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		return VO;
	}

}
