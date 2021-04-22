package br.com.flow.trocaDeGrade;

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

public class flow_t_grade_TarefaJava_AlteraContrato implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		Object idFlow = arg0.getIdInstanceProcesso();
		String patrimonio = getPatrimonio(idFlow);
		deletarTeclasContrato(patrimonio);
		getTeclas(idFlow);
	}

	public String getPatrimonio(Object idFlow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_MAQUINASTGRADE");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?", new Object[] { idFlow });
		String patrimonio = VO.asString("CODBEM");

		return patrimonio;
	}

	public void deletarTeclasContrato(String patrimonio) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "this.CODBEM=?", new Object[] { patrimonio }));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void getTeclas(Object idFlow) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_GRADEFUTURA", "this.IDINSTPRN = ? ", new Object[] { idFlow }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				inserirTecla(DynamicVO);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void inserirTecla(DynamicVO DynamicVO) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("teclas");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("NUMCONTRATO", numcontrato);
			VO.setProperty("CODPROD", codprod);

			dwfFacade.createEntity("CertificacaoRegra", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
