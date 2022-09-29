package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal usuarioInclusao = VO.asBigDecimal("CODUSUINC");
		
		if(usuarioInclusao.intValue() == 3538) {
			BigDecimal tipoPagamento = VO.asBigDecimal("CODTIPVENDA");
			if(tipoPagamento.intValue() == 1333) {
				
			}
		}
		
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
		update(arg0);
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal top = VO.asBigDecimal("CODTIPOPER");
		if(top.intValue()==1101) {
			String idPedidoVtex = VO.asString("AD_PEDIDOVTEX");
			if(idPedidoVtex!=null) {
				
				String invoicekey = VO.asString("CHAVENFE");
						
				if(invoicekey!=null) {
					atualStatus(VO, invoicekey, idPedidoVtex);
				}
				
			}
		}
	}
	
	private void atualStatus(DynamicVO VO, String invoicekey, String idPedidoVtex) {
		BigDecimal nroUnico = VO.asBigDecimal("NUNOTA");
		Timestamp issuancedate = VO.asTimestamp("DTNEG");
		BigDecimal invoicevalue = VO.asBigDecimal("VLRNOTA");
		String statusnfe = VO.asString("STATUSNFE");
		BigDecimal invoicenumber = VO.asBigDecimal("NUMNOTA");
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_STSECOMM",
					"this.AD_PEDIDOVTEX=?", new Object[] { idPedidoVtex }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VOS = (DynamicVO) NVO;
				
				VOS.setProperty("NUNOTA", nroUnico);
				VOS.setProperty("INVOICEKEY", invoicekey);
				VOS.setProperty("ISSUANCEDATE", issuancedate);
				VOS.setProperty("INVOICEVALUE", invoicevalue);
				VOS.setProperty("STATUSNFE", statusnfe);
				VOS.setProperty("INVOICENUMBER", invoicenumber);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal usuarioInclusao = VO.asBigDecimal("CODUSUINC");
		BigDecimal top = VO.asBigDecimal("CODTIPOPER");
		String idVtex = VO.asString("AD_PEDIDOVTEX");
		BigDecimal tipoPagamento = VO.asBigDecimal("CODTIPVENDA");

		if (usuarioInclusao.intValue() == 3538) { // usuario e-commerce
			if (idVtex != null) {
				if (verificaSeJaExiste(top, idVtex)) {
					cadastraLog(idVtex);
					throw new Error("Pedido " + idVtex + " já existe, não pode ser cadastrado novamente !");
				}
				
				//TODO :: muda o tipo de pagamento promissória.
	        	if(tipoPagamento.intValue()==1333) {
	        		BigDecimal parceiro = VO.asBigDecimal("CODPARC");
	        		DynamicVO tgfcpl = getTGFCPL(parceiro);
	        		if(tgfcpl!=null) {
	        			BigDecimal sugestaoTipNeg = BigDecimalUtil.getValueOrZero(tgfcpl.asBigDecimal("SUGTIPNEGSAID"));
	        			if(sugestaoTipNeg.intValue()>0) {
	        				VO.setProperty("CODTIPVENDA", sugestaoTipNeg);
	        			}
	        		}
	        	}
				
				//TODO :: Verificar se tem observações adicionais
				String observacaoAtual = VO.asString("OBSERVACAO");
				String obsAdicional = verificaObservacaoAdicional();
				if(obsAdicional!=null && obsAdicional!="") {
					String newObs = observacaoAtual+" "+obsAdicional;
					VO.setProperty("OBSERVACAO", newObs);
				}
				
				//TODO :: Ajustar frete 03/06/22
				VO.setProperty("CIF_FOB", "C");
				VO.setProperty("TIPFRETE", "S");
				VO.setProperty("VENCFRETE", TimeUtils.dataAddDay(TimeUtils.getNow(), 30));
			}
		}
		
	}
	
	private DynamicVO getTGFCPL(BigDecimal parceiro) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ComplementoParc");
		DynamicVO VO = DAO.findOne("CODPARC=?",new Object[] { parceiro });
		return VO;
	}
	
	private String verificaObservacaoAdicional() {
		String obs="";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_CONFIGECOMM");
			DynamicVO VO = DAO.findOne("ID=?",new Object[] { new BigDecimal(1) });
			
			if(VO!=null) {
				String asString = VO.asString("OBSNOTA");
				
				if(asString!=null) {
					obs = asString;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return obs;
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
