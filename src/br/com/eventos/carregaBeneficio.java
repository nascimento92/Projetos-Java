package br.com.eventos;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;



public class carregaBeneficio implements EventoProgramavelJava {
	
	
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		carregabeneficio( arg0 );
	}


	
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		carregabeneficio( arg0 );
	}


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
	
	
	
	// Metodos privados 
	
	private void carregabeneficio(PersistenceEvent arg0) throws Exception {
		
		//try {
			
			DynamicVO itemVO = (DynamicVO) arg0.getVo();
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			// Item da Nota
			BigDecimal nunota   = itemVO.asBigDecimal("NUNOTA");
			BigDecimal codProd  = itemVO.asBigDecimal("CODPROD");
			BigDecimal codEmp   = itemVO.asBigDecimal("CODEMP");
			Object codTribut  = itemVO.getProperty("CODTRIB");
			BigDecimal codTrib = new BigDecimal(0);
			Object CodBen =itemVO.getProperty("CODBENEFNAUF"); 
			String CodBeneficio = null;
			
			if (CodBen!=null) {
				CodBeneficio =  CodBen.toString();
			}
			
			if (codTribut!=null) {
				codTrib =  (BigDecimal) codTribut;
			}
			
			// Produto
			DynamicVO ProdutoVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("Produto",codProd);
			BigDecimal grupoICMSprod = ProdutoVO.asBigDecimal("GRUPOICMS");
			// Cabeçalho da Nota
			DynamicVO CabVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota",nunota);
			BigDecimal codParc = CabVO.asBigDecimal("CODPARC");	
			BigDecimal codUF = obtemUFParceiro(codParc);
			String TipoMovimento = CabVO.asString("TIPMOV");

		
			// Beneficio
			if (CodBeneficio==null && TipoMovimento.contentEquals("T") && grupoICMSprod!=null) {
				//System.out.println("Produto: "+codProd+" Empresa:"+codEmp.toString()+" Estado:"+codUF.toString()+" Grupo ICMS:"+grupoICMSprod.toString()+ " Tributação:"+codTrib.toString() + " Tipo de Movimento:"+TipoMovimento + " Beneficio:"+CodBeneficio);

				CodBeneficio = obtemBeneficio(codEmp,codUF,grupoICMSprod,codTrib);
				
				if(CodBeneficio!=null){
					itemVO.setProperty("CODBENEFNAUF", CodBeneficio);
				}
				
			};
			
		//} catch (Exception e) {
		//	System.out.println("**** Não foi possivel carregar os beneficios da nota: "+nunota+e.getMessage());
		//}
		

	}

    private static String obtemBeneficio(BigDecimal codEmp, BigDecimal codUF, BigDecimal grupoICMSprod, BigDecimal codTrib) throws Exception {
    	String codBeneficio = null;

		JapeWrapper BeneficioDAO = null;
		BeneficioDAO = JapeFactory.dao("Beneficios");
    	DynamicVO Beneficios=null;
    	Beneficios = BeneficioDAO.findOne(""
    						+ " 	CODEMP=? AND CODUFDEST=? AND GRUPOICMS=? AND CODTRIB=? ",
    							new Object[] {
    									codEmp,
    									codUF,
    									grupoICMSprod,
    									codTrib
    									  });
    				
    				if(Beneficios!=null) {
    					codBeneficio = Beneficios.asString("CODBENEFNAUF") ;
     				}

    	return codBeneficio;
    }


    private static BigDecimal obtemUFParceiro ( BigDecimal CodParc ) throws Exception {
    	BigDecimal codUF = new BigDecimal (0);
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO ParceiroVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("Parceiro",CodParc);

		BigDecimal codCid = ParceiroVO.asBigDecimal("CODCID"); 
		DynamicVO CidadeVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("Cidade",codCid);
		
		if (CidadeVO!= null) {
			codUF = CidadeVO.asBigDecimal("UF");
		}
     	
    	return codUF;
    }



}




