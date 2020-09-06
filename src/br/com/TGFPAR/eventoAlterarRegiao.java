package br.com.TGFPAR;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoAlterarRegiao implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		try {
			star(arg0);
		} catch (Exception e) {
			System.out.println("-#-#-> Não foi possivel alterar a região da cidade "+e.getMessage());
		}
		
	}
	
	private void star(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal newRegiao = newVO.asBigDecimal("CODREG");
		BigDecimal oldRegiao = oldVO.asBigDecimal("CODREG");
		
		BigDecimal cidade = newVO.asBigDecimal("CODCID");
		
		if(newRegiao!=oldRegiao) {
			alteraRegiaoCidade(cidade,newRegiao);
		}
	}
	
	private void alteraRegiaoCidade(BigDecimal cidade, BigDecimal regiao) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("Cidade", cidade);
		EntityVO NVO = PersistentLocalEntity.getValueObject();
		DynamicVO VO = (DynamicVO) NVO;
		
		BigDecimal regiaoCidade = VO.asBigDecimal("CODREG");
		
		if(regiao!=regiaoCidade) {
			VO.setProperty("CODREG", regiao);
		}

		PersistentLocalEntity.setValueObject((EntityVO) VO);

	}
	
}
