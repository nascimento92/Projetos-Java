package br.com.eventos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

/**
 * 
 * @author gabriel.nascimento
 *
 * 12/09/2019 - Objeto para gravar todas as vezes que se cadastra um usuário as regras da BEC
 */
public class eventoCadastroNovosUsuarios implements EventoProgramavelJava {

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
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception{
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal codusu = VO.asBigDecimal("CODUSU");
		
		salvaRegras(codusu);
		
	}
	
	private void salvaRegras(BigDecimal codusu) throws Exception{
		
		List<BigDecimal> regras = new ArrayList<BigDecimal>();
		regras.add(new BigDecimal(64));
		regras.add(new BigDecimal(54));
		regras.add(new BigDecimal(58));
		regras.add(new BigDecimal(56));
		regras.add(new BigDecimal(67));
		
		
		int sequencia = 20;
		
		for(int i =0; i<regras.size(); i++){
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("CertificacaoRegra");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("ATIVO", "S");
			VO.setProperty("CHAVE", codusu);
			VO.setProperty("CODREGRA", regras.get(i));
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("FILTRAR", "N");
			VO.setProperty("RECONTAR", "S");
			VO.setProperty("SEQUENCIA", new BigDecimal(sequencia));
			VO.setProperty("TIPO", "U");
			
			dwfFacade.createEntity("CertificacaoRegra", (EntityVO) NPVO);
			
			sequencia++;
		}
		
		
	}

}

