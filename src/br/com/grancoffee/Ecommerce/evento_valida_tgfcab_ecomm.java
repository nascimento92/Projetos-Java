package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_valida_tgfcab_ecomm implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

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
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal usuarioInclusao = VO.asBigDecimal("CODUSUINC");
		BigDecimal top = VO.asBigDecimal("CODTIPOPER");
		String idVtex = VO.asString("AD_PEDIDOVTEX");

		if (usuarioInclusao.intValue() == 3538) { // usuario e-commerce
			if (idVtex != null) {
				if (verificaSeJaExiste(top, idVtex)) {
					cadastraLog(idVtex);
					throw new Error("Pedido " + idVtex + " já existe, não pode ser cadastrado novamente !");
				}
				
				String sigla = idVtex.substring(0,3);
				
				if("MGL".equals(sigla)) {
					VO.setProperty("CODTIPVENDA", new BigDecimal(1265));
				}
			}
		}
	}

	private boolean verificaSeJaExiste(BigDecimal top, String pedidoVtex) {
		boolean valida = false;

		try {
			JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
			DynamicVO VO = DAO.findOne("CODTIPOPER=? AND AD_PEDIDOVTEX=?", new Object[] { top, pedidoVtex });
			if (VO != null) {
				valida = true;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return valida;

	}

	private void cadastraLog(String idVtex) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("TABELA", "TGFCAB");
			VO.setProperty("CAMPO", "AD_PEDIDOVTEX");
			VO.setProperty("DTALTER", TimeUtils.getNow());
			VO.setProperty("PKTABELA", idVtex);
			VO.setProperty("OBSERVACAO", "Pedido VTEX "+idVtex+" impedido de ser integrado! Por motivo de duplicidade!");

			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
