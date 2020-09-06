package br.com.OrdemServico;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class eventoAcertaOperacaoOS implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
		
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
		
		String codbem = VO.asString("SERIE");
		BigDecimal usuario = VO.asBigDecimal("CODUSU");
		
		if(codbem!=null && usuario!=null){
			
			//if(usuario.intValue()==815){

				DynamicVO enderecamento = getEnderecamento(codbem);
				BigDecimal idPlanta = enderecamento.asBigDecimal("ID");
				BigDecimal contrato = enderecamento.asBigDecimal("NUMCONTRATO");
				BigDecimal operacao = getOperacao(idPlanta,contrato);
								
				switch(operacao.intValue()){
				case 39:
					VO.setProperty("CODUSU", new BigDecimal(299)); break;
				case 40:
					VO.setProperty("CODUSU", new BigDecimal(58)); break;
				case 41:
					VO.setProperty("CODUSU", new BigDecimal(57)); break;
				case 42:
					VO.setProperty("CODUSU", new BigDecimal(56)); break;
				case 43:
					VO.setProperty("CODUSU", new BigDecimal(60)); break;
				case 44:
					VO.setProperty("CODUSU", new BigDecimal(615)); break;
				case 45:
					VO.setProperty("CODUSU", new BigDecimal(471)); break;
				case 46:
					VO.setProperty("CODUSU", new BigDecimal(186)); break;
				case 50:
					VO.setProperty("CODUSU", new BigDecimal(772)); break;
				case 54:
					VO.setProperty("CODUSU", new BigDecimal(956)); break;
				case 55:
					VO.setProperty("CODUSU", new BigDecimal(57)); break;
				case 58:
					VO.setProperty("CODUSU", new BigDecimal(951)); break;
				case 59:
					VO.setProperty("CODUSU", new BigDecimal(1319)); break;
				case 69:
					VO.setProperty("CODUSU", new BigDecimal(1318)); break;
					default:
						VO.setProperty("CODUSU", new BigDecimal(56));
				//}
			}
		}

	}
	
	private DynamicVO getEnderecamento(String codbem) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });	
		return VO;

	}
	
	private BigDecimal getOperacao(BigDecimal id, BigDecimal contrato) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("ID=? AND NUMCONTRATO=?",new Object[] { id, contrato });
		
		BigDecimal operacao = VO.asBigDecimal("AD_GRPOPER");
		
		if(operacao.equals(null)){
			operacao = new BigDecimal(56);
		}
		
		return operacao;
	}
	
}
