package br.com.TCIBEM;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_vincularBemAoContrato implements AcaoRotinaJava {
	
	/**
	 * @author gabriel.nascimento
	 * Objeto para vincular um bem a um contrato.
	 */
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		if (linhas.length == 1) {
			Integer contrato = (Integer) arg0.getParam("NUMCONTRATO");
			start(linhas, contrato, arg0);
		} else {
			arg0.setMensagemRetorno("SELECIONE APENAS UM BEM !");
		}
	}

	public void start(Registro[] linhas, Integer contrato, ContextoAcao arg0) throws Exception {
		String patrimonio = (String) linhas[0].getCampo("CODBEM");
		alteraContrato(patrimonio, contrato, arg0);

		if (existeNaTGFEST(patrimonio)) {
			alteraNaTGFEST(patrimonio,contrato);
		}

	}

	public void alteraContrato(String patrimonio, Integer contrato, ContextoAcao arg0) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("Imobilizado", "this.CODBEM=? ", new Object[] { patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("NUMCONTRATO", new BigDecimal(contrato));

				itemEntity.setValueObject(NVO);
			}

			arg0.setMensagemRetorno("BEM VINCULADO AO CONTRATO!");

		} catch (Exception e) {
			arg0.setMensagemRetorno("NAO FOI POSSIVEL VINCULAR O PATRIMONIO AO CONTRATO!\n\n" + e.getMessage());
		}
	}

	public boolean existeNaTGFEST(String patrimonio) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("CONTROLE=?", new Object[] { patrimonio });

		if (VO != null) {
			valida = true;
		}

		return valida;
	}

	public void alteraNaTGFEST(String patrimonio, Integer contrato) throws Exception {
		
		DynamicVO TCSCON = TCSCON(contrato);
		
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Estoque",
					"this.controle=?", new Object[] { patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODPARC", TCSCON.asBigDecimal("CODPARC"));

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			System.out.println("NAO FOI POSSIVEL ALTERAR A TGFEST!\n\n" + e.getMessage());
		}
	}
	
	public DynamicVO TCSCON(Integer contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		return VO;
	}
	

}
