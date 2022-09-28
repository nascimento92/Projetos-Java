package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.sankhya.util.BigDecimalUtil;
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

public class evento_valida_dados_ecomm implements EventoProgramavelJava{

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
	
	public void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO)arg0.getVo();
	    BigDecimal nunota = VO.asBigDecimal("NUNOTA");
	    BigDecimal produto = VO.asBigDecimal("CODPROD");
	    String codvol = VO.asString("CODVOL");
	    BigDecimal quantidadeOriginal = VO.asBigDecimal("QTDNEG");
	    BigDecimal valorOriginal = VO.asBigDecimal("VLRUNIT");
	    BigDecimal novocodlocalorig = new BigDecimal(1117);
	    
	    if (nunota != null) {
	      DynamicVO tgfcab = getTGFCAB(nunota);
	      if (tgfcab != null) {
	        BigDecimal usuarioInclusao = tgfcab.asBigDecimal("CODUSUINC");
	        if (usuarioInclusao.intValue() == 3538) {
	        //if (usuarioInclusao.intValue() == 648) {
	          DynamicVO tgfpro = getTGFPRO(produto);
	          if (tgfpro != null) {
	        	  
	        	//TODO::Verifica se é uma máquina 
	        	BigDecimal grupoProduto = tgfpro.asBigDecimal("CODGRUPOPROD");
	        	if(grupoProduto.intValue()>=500000 && grupoProduto.intValue()<600000) {//é uma máquina
	        		VO.setProperty("CODLOCALORIG", novocodlocalorig);
	        	}
	        	
	        	if(produto.intValue()==515399) {
	        		VO.setProperty("CODLOCALORIG", novocodlocalorig);
	        	}
	        	
	        	BigDecimal quantidade = null;
	        	  
	        	//TODO::Verifica a unidade e-commerce.  
	            String unidadeVtex = tgfpro.asString("AD_UNIDADELV");
	            if (!codvol.equals(unidadeVtex)) {
	            	quantidade = getQuantidade(produto, unidadeVtex); //10
	              if (quantidade != null) {
	                VO.setProperty("QTDNEG", quantidadeOriginal.multiply(quantidade));
	                VO.setProperty("CODVOL", unidadeVtex);
	                VO.setProperty("VLRUNIT", valorOriginal.divide(quantidade, 2, RoundingMode.HALF_EVEN));
	              } 
	            }
	            
	            //TODO::Registra Log
	            String idVtex = tgfcab.asString("AD_PEDIDOVTEX");
	            if(idVtex!=null) {
	            	
	            	if(quantidade==null) {
	            		quantidade = new BigDecimal(1);
	            	}
	            	
	            	BigDecimal quantiLog = BigDecimalUtil.getValueOrZero(quantidadeOriginal.multiply(quantidade));
	            	BigDecimal valorLog = BigDecimalUtil.getValueOrZero(valorOriginal.divide(quantidade, 2, RoundingMode.HALF_EVEN));
	            	String obs = "Pro: "+produto+", Vol vx: "+codvol+", Vol pro: "+unidadeVtex+", qtd vx: "+quantidadeOriginal+", qtd n: "+quantiLog+", vlr vx: "+valorOriginal+", vlr n: "+valorLog;
	            	cadastraLog(idVtex,obs);
	            }
	            
	            //TODO:: Registra na AD_STATUSECOMM
	            BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
	            if(top.intValue()==1070) { //pedido
	            	inserirStatus (VO, tgfcab);
	            }
	            
	          } 
	        } 
	      } 
	    }
	    
	}
	
	private void inserirStatus(DynamicVO itemVO, DynamicVO cabVO) {
		BigDecimal pedido = cabVO.asBigDecimal("NUNOTA");
		BigDecimal sequencia = itemVO.asBigDecimal("SEQUENCIA");
		BigDecimal id = itemVO.asBigDecimal("CODPROD");
		BigDecimal quantity = itemVO.asBigDecimal("QTDNEG");
		BigDecimal price = itemVO.asBigDecimal("VLRUNIT");
		String idPedidoVtex = cabVO.asString("AD_PEDIDOVTEX");
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_STSECOMM");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("PEDIDO", pedido);
			VO.setProperty("SEQUENCIA", sequencia);
			VO.setProperty("ID", id);
			VO.setProperty("QUANTITY", quantity);
			VO.setProperty("PRICE", price);
			VO.setProperty("AD_PEDIDOVTEX", idPedidoVtex);
			
			dwfFacade.createEntity("AD_STSECOMM", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[inserirStatus] não foi possível cadastrar o item! ID Pedido: "+idPedidoVtex+" produto "+id+"\n"+e.getCause()+"\n"+e.getMessage());
		}
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}
			
	public BigDecimal getQuantidade(BigDecimal produto, String unidade) {
		BigDecimal qtd = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("VolumeAlternativo");
			DynamicVO VO = DAO.findOne("CODPROD=? AND CODVOL=?",new Object[] { produto,unidade });
			
			if(VO!=null) {
				BigDecimal quantidade = VO.asBigDecimal("QUANTIDADE");
				
				if(unidade!=null) {
					qtd = quantidade;
				}
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return qtd;
	}
	
	private void cadastraLog(String idVtex, String obs) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("TABELA", "TGFITE");
			VO.setProperty("CAMPO", "AD_PEDIDOVTEX");
			VO.setProperty("DTALTER", TimeUtils.getNow());
			VO.setProperty("PKTABELA", idVtex);
			VO.setProperty("OBSERVACAO", obs);

			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);

		} catch (Exception e) {
			salvarException("[cadastraLog] não foi possível cadastrar! ID Pedido: "+idVtex+"\n"+e.getCause()+"\n"+e.getMessage());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_valida_dados_ecomm");
			VO.setProperty("PACOTE", "br.com.grancoffee.Ecommerce");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}






















