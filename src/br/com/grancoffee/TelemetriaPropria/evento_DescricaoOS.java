package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

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

public class evento_DescricaoOS implements EventoProgramavelJava {
	
	/**
	 * Evento temporario inserido na GC_SOLICITABAST para quando for gerado uma OS ele criar a descrição informando tecla/produto/capacidade/nivelpar/quantidade/valor
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		String patrimonio = VO.asString("CODBEM");
		
		if(numos!=null) {
			vericaQuaisProdutos(nunota,patrimonio,numos);
		}
	}
	
	private void vericaQuaisProdutos(BigDecimal nunota, String patrimonio,BigDecimal numos) {
		try {
			
			String descricao="";
			String legenda="ABASTECER: "+patrimonio+" \n\n";
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ItemNota", "this.NUNOTA = ? ", new Object[] { nunota }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				
				if(DynamicVO!=null) {
					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
					DynamicVO pLanograma = getPLanograma(patrimonio,produto);
					DynamicVO tgfPro = getTgfPro(produto);
					
					descricao+=
							"TECLA: "+pLanograma.asString("TECLA")+"| "+
							"PRODUTO: "+tgfPro.asString("DESCRPROD")+"| "+
							"CAPACIDADE: "+pLanograma.asBigDecimal("CAPACIDADE")+"| "+
							"NIVEL PAR: "+pLanograma.asBigDecimal("NIVELPAR")+"| "+
							"QUANTIDADE: "+ DynamicVO.asBigDecimal("QTDNEG")+"| "+
							"VALOR: R$"+DynamicVO.asBigDecimal("VLRUNIT")+"\n\n";
				}
			}
			
			String descrfinal = legenda+descricao;
			atualizaDescricao(descrfinal,numos);
			
		} catch (Exception e) {
			System.out.println("## [evento_DescricaoOS] ## - Nao foi possivel verificar os itens da nota!");
			e.getMessage();
			e.getCause();
		}
	}
	
	private void atualizaDescricao(String descrfinal, BigDecimal numos) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("DESCRICAO", descrfinal);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			System.out.println("## [evento_DescricaoOS] ## - Nao foi possivel atualizar a descricao da OS!");
			e.getMessage();
			e.getCause();
		}
	}
	
	private DynamicVO getPLanograma(String patrimonio, BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCPlanograma");
		DynamicVO VO = DAO.findOne("CODBEM=? and CODPROD=?",new Object[] { patrimonio, produto });
		return VO;
	}
	
	private DynamicVO getTgfPro(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}

}
