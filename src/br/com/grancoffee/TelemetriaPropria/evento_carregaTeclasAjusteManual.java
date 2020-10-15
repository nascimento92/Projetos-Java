package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_carregaTeclasAjusteManual implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("STATUS", "1");

		String patrimonio = VO.asString("CODBEM");
		BigDecimal idAjuste = VO.asBigDecimal("ID");
		
		carregaTeclas(patrimonio,idAjuste);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("STATUS", "1");
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void carregaTeclas(String patrimonio, BigDecimal idAjuste) throws Exception {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
			BigDecimal nivelPar = DynamicVO.asBigDecimal("NIVELPAR");

			try {

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSAJUSTESMANUAIS");
				DynamicVO VO = (DynamicVO) NPVO;

				VO.setProperty("ID", idAjuste);
				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("TECLA", tecla);
				VO.setProperty("CODPROD", produto);
				VO.setProperty("CAPACIDADE", capacidade);
				VO.setProperty("NIVELPAR", nivelPar);
				VO.setProperty("AJUSTADO", "N");
				VO.setProperty("SALDOANTES", DynamicVO.asBigDecimal("ESTOQUE"));

				dwfFacade.createEntity("AD_ITENSAJUSTESMANUAIS", (EntityVO) VO);

			} catch (Exception e) {
				System.out.println(
						"## [evento_carregaTeclasAjusteManual] ## - Nao foi possivel salvar as teclas na tela Retornos Abastecimento!");
				e.getMessage();
				e.printStackTrace();
			}

		}
	}
}
