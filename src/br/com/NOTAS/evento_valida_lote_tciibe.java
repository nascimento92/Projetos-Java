package br.com.NOTAS;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_valida_lote_tciibe implements EventoProgramavelJava {
	
	/**
	 * @author gabriel.nascimento
	 * 
	 * Localizado na tciibe.
	 * 
	 * Objeto para verificar se o patrimonio informado é o mesmo do lote.
	 * 
	 * 1° valida se a top é de remessa e movimenta bem.
	 * 2° validar se o produto é controlado por lote.
	 * 3° validar se aquele lote já foi utilizado outras vezes.
	 * 
	 */
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		String patrimonio = VO.asString("CODBEM");
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		
		//Pega o campo atualiza bem da nota.
		String atualizaBem = verificaSeAhTopMovimentaBem(nunota);
		
		//verifica se a top valida bem
		if("T".equals(atualizaBem) || "C".equals(atualizaBem)) {//se for uma top Trans. saída/Remessa e Compra
			
			String controleAdicional = verificaSeOhProdutoEhControladoPorLote(produto);
			
			if("L".equals(controleAdicional)) {//valida se o produto é controlado por lote
				
				if(!validaLote(nunota,produto,patrimonio)) {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>ERRO - PATRIMONIO DIFERENTE DO LOTE.</b></font>\n\n\n <b>A numeração do lote e do patrimônio para este produto devem ser iguais!</b>");
				}		
			}	
		}
	}
	
	
	public String verificaSeAhTopMovimentaBem(BigDecimal nunota) throws Exception {
		DynamicVO tgfcab = getTGFCAB(nunota);
		BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
		DynamicVO tgftop = ComercialUtils.getTipoOperacao(top);
		String atualizaBem = tgftop.asString("ATUALBEM");
		
		return atualizaBem;
	}
	
	public DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	public String verificaSeOhProdutoEhControladoPorLote(BigDecimal codprod) throws Exception {
		DynamicVO tgfpro = getTGFPRO(codprod);
		String controleEstoque = tgfpro.asString("TIPCONTEST");
		return controleEstoque;
	}
	
	public DynamicVO getTGFPRO(BigDecimal codprod) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { codprod });
		return VO;
	}
	
	public boolean validaLote(BigDecimal nunota,BigDecimal produto,String patrimonio) throws Exception {
		
		boolean valida = false;

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("ItemNota", "this.NUNOTA=? AND this.CODPROD=? ", new Object[] { nunota, produto }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String controle = DynamicVO.asString("CONTROLE");
			if (controle.equals(patrimonio)) {
				valida=true;
			}
		}
		
		return valida;
	}
}
