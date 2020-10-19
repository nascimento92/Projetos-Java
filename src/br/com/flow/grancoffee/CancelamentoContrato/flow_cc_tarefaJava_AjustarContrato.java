package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_AjustarContrato implements TarefaJava {
	
	/**
	 * 
	 */
	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoTarefa arg0) throws Exception {
		Object idflow = arg0.getIdInstanceProcesso();
		DynamicVO form = getForm(idflow);
		
		BigDecimal contrato = form.asBigDecimal("NUMCONTRATO");
		
		verificarPatrimonios(idflow,contrato);
	}
	
	private DynamicVO getForm(Object idflow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idflow });
		return VO;
	}
	
	private void verificarPatrimonios(Object idflow, BigDecimal contrato) {
		String patrimonio = "";
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_PATCANCELAMENTO", "this.IDINSTPRN = ? ", new Object[] { idflow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				if (DynamicVO != null) {
					if(DynamicVO.asString("CODBEMRECEBIDO")!=null) {
						patrimonio = DynamicVO.asString("CODBEMRECEBIDO");
					}else {
						patrimonio = DynamicVO.asString("CODBEM");
					}
					
					retirarPatrimonio(patrimonio,contrato);
				}
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_AjustarContrato] ## - Nao foi possivel verificar o patrimonio para ajustar!");
			e.getMessage();
			e.getCause();
		}
	}
	
	private void retirarPatrimonio(String patrimonio, BigDecimal contrato) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("Imobilizado", patrimonio);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;

			appVO.setProperty("NUMCONTRATO", new BigDecimal(0));
	
			PersistentLocalEntity.setValueObject(NVO);
			
			if(appVO.asBigDecimal("NUMCONTRATO").intValue()==0) {
				try {
					
					dwfFacade = EntityFacadeFactory.getDWFFacade();
					dwfFacade.removeByCriteria(new FinderWrapper("ENDERECAMENTO", "this.CODBEM=? and this.NUMCONTRATO=?",new Object[] {patrimonio,contrato}));
					
				} catch (Exception e) {
					System.out.println("## [flow_cc_tarefaJava_AjustarContrato] ## - Nao foi possivel deletar o enderecamento!"+ e.getMessage());
					e.getMessage();
					e.getCause();
				}
			}
	
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_AjustarContrato] ## - Nao foi possivel retirar patrimonio do contrato!"+ e.getMessage());
			e.getMessage();
			e.getCause();
		}
	}
}
