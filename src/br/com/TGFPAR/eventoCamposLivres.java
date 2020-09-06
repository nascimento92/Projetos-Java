package br.com.TGFPAR;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class eventoCamposLivres implements EventoProgramavelJava{

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		
		try {
			insert(arg0);	
		} catch (Exception e) {
			System.out.println("-*-*-> Não foi possivel alterar as informações livres "+e.getMessage());
		}
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		try {
			update(arg0);
		} catch (Exception e) {
			System.out.println("-*-*-> Não foi possivel alterar as informações livres "+e.getMessage());
		}
		
	}
	
	private void update(PersistenceEvent arg0) {
		
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal vendedorNew = newVO.asBigDecimal("AD_CODVENDLIVRE");
		BigDecimal vendedorOld = oldVO.asBigDecimal("AD_CODVENDLIVRE");
		
		BigDecimal vendedorPrefNew = newVO.asBigDecimal("CODVEND");
		BigDecimal vendedorPrefOld = oldVO.asBigDecimal("CODVEND");
		
		if(vendedorNew!=vendedorOld && vendedorNew!=null) {
			alteraVendedor(vendedorNew,newVO);
		}
		
		if(vendedorPrefNew!=vendedorPrefOld) {
			alterarVendedorLivre(vendedorPrefNew,newVO);
		}
		
		String grupoautorLivreNew = newVO.asString("AD_GRUPOAUTORLIVRE");
		String grupoautorLivreOld = oldVO.asString("AD_GRUPOAUTORLIVRE");
		
		String grupoautorNew = newVO.asString("GRUPOAUTOR");
		String grupoautorOld = oldVO.asString("GRUPOAUTOR");
		
		if(grupoautorLivreNew!=grupoautorLivreOld && grupoautorLivreNew!=null) {
			alteraGrupoAutorizacao(grupoautorLivreNew,newVO);
		}
		
		if(grupoautorNew!=grupoautorOld) {
			alteraGrupoAutorizacaoLivre(grupoautorNew,newVO);
		}
	}
	
	public void insert(PersistenceEvent arg0) {
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		BigDecimal vendedorLivre = VO.asBigDecimal("AD_CODVENDLIVRE");
		BigDecimal vendedor = VO.asBigDecimal("CODVEND");
		
		if(vendedorLivre!=null) {
			alteraVendedor(vendedorLivre,VO);
		}
		
		if(vendedor!=null) {
			alterarVendedorLivre(vendedor,VO);
		}
		
		String grupoAutorLivre = VO.asString("AD_GRUPOAUTORLIVRE");
		String grupoAutor = VO.asString("GRUPOAUTOR");
		
		if(grupoAutorLivre!=null) {
			alteraGrupoAutorizacao(grupoAutorLivre,VO);
		}
		
		if(grupoAutor!=null) {
			alteraGrupoAutorizacaoLivre(grupoAutor,VO);
		}
		
	}
	
	private void alteraVendedor(BigDecimal vendedor,DynamicVO VO) {
		
		VO.setProperty("CODVEND", vendedor);
	}
	
	private void alterarVendedorLivre(BigDecimal vendedor,DynamicVO VO) {
		
		VO.setProperty("AD_CODVENDLIVRE", vendedor);
	}
	
	private void alteraGrupoAutorizacao(String grupoautor,DynamicVO VO) {
		
		VO.setProperty("GRUPOAUTOR", grupoautor);
	}
	
	private void alteraGrupoAutorizacaoLivre(String grupoautor,DynamicVO VO) {
		
		VO.setProperty("AD_GRUPOAUTORLIVRE", grupoautor);
	}
}
