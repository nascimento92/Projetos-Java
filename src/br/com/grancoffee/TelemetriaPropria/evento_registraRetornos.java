package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_registraRetornos implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		delete(arg0);
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal idTelaRetorno = getIdTelaRetorno(numos);
		BigDecimal codprod = VO.asBigDecimal("CODPROD");
		BigDecimal qtd = VO.asBigDecimal("QTD");
		BigDecimal idretorno = VO.asBigDecimal("IDRETORNO");
		String tecla = VO.asString("TECLA");
		
		alterarRetorno(codprod,idTelaRetorno,idretorno,tecla,qtd);
	}

	private void insert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal idTelaRetorno = getIdTelaRetorno(numos);
		BigDecimal codprod = VO.asBigDecimal("CODPROD");
		BigDecimal qtd = VO.asBigDecimal("QTD");
		BigDecimal idretorno = VO.asBigDecimal("IDRETORNO");
		String tecla = VO.asString("TECLA");

		cadastrarRetorno(codprod, idretorno, idTelaRetorno, qtd, tecla);
	}
	
	private void delete(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal idTelaRetorno = getIdTelaRetorno(numos);
		BigDecimal codprod = VO.asBigDecimal("CODPROD");
		BigDecimal idretorno = VO.asBigDecimal("IDRETORNO");
		String tecla = VO.asString("TECLA");
		
		deletaRetorno(idTelaRetorno,codprod,tecla,idretorno);
	}

	private BigDecimal getIdTelaRetorno(BigDecimal numos) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
		DynamicVO VO = DAO.findOne("NUMOS=?", new Object[] { numos });
		BigDecimal id = VO.asBigDecimal("ID");
		return id;
	}

	private void cadastrarRetorno(BigDecimal codprod, BigDecimal idretorno, BigDecimal idTelaRetorno, BigDecimal qtd,
			String tecla) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_PRODRETABAST");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODPROD", codprod);
			VO.setProperty("IDRETORNO", idretorno);
			VO.setProperty("ID", idTelaRetorno);
			VO.setProperty("QTD", qtd);
			VO.setProperty("TECLA", tecla);
			dwfFacade.createEntity("AD_PRODRETABAST", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[cadastrarRetorno] Nao foi possivel salvar o retorno! ID Tela Retorno: " + idretorno
					+ " produto: " + codprod + "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void alterarRetorno(BigDecimal codprod, BigDecimal idTelaRetorno, BigDecimal idretorno, String tecla, BigDecimal quantidade) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PRODRETABAST",
					"this.CODPROD=? AND this.ID=? AND this.IDRETORNO=? AND this.TECLA=? ", new Object[] { codprod,idTelaRetorno, idretorno, tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("QTD", quantidade);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[alterarRetorno] Nao foi possivel salvar a alteração! ID Tela Retorno: " + idretorno
					+ " produto: " + codprod + "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}

	private void deletaRetorno(BigDecimal idTelaRetorno,BigDecimal codprod,String tecla, BigDecimal idretorno) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PRODRETABAST", "this.ID=? AND this.CODPROD=? AND this.TECLA=? AND this.IDRETORNO=?",new Object[] {idTelaRetorno,codprod,tecla,idretorno}));
		} catch (Exception e) {
			salvarException("[alterarRetorno] Nao foi possivel excluir o produto! ID Tela Retorno: " + idTelaRetorno
					+ " produto: " + codprod + "\n" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_registraRetornos");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
