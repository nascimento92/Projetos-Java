package br.com.flow.RelatorioInstalacao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_5_VerificaAreaTecnica implements TarefaJava {
	/**
	 * Não utilizado
	 */
	private String tarefa = "UserTask_059qlxl";
	private String processo = "5";
	
	public void executar(ContextoTarefa contexto) throws Exception {
		
		start(contexto);
		
	}
	
	private void start(ContextoTarefa contexto) throws Exception {
		Object unidade = contexto.getCampo("UNIDADE");
		
		if(unidade==null) {
			unidade = 1;
		}
		
		if(validaSeExisteRegistro()) {
			atualizaRegistro(unidade);
		}
	}
	
	private boolean validaSeExisteRegistro() throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("UsuarioCandidatoProcesso");
		DynamicVO VO = DAO.findOne("IDELEMENTO=? AND CODPRN=?",new Object[] { tarefa,processo });
		if(VO!=null) {valida = true;}
		return valida;
	}
	
	private void atualizaRegistro(Object unidade) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("UsuarioCandidatoProcesso",
				"this.IDELEMENTO=? AND this.CODPRN=? ", new Object[] { tarefa, processo }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;
			
			BigDecimal equipe = validaUnidade(unidade);
			
			VO.setProperty("CODEQUIPE", equipe);

			itemEntity.setValueObject(NVO);

		}
	}
	
	private BigDecimal validaUnidade(Object un) {
		BigDecimal unidade = getBigDecimal(un);
		BigDecimal equipe = null;

		if(unidade.intValue()==1) {
			equipe = new BigDecimal(1);
		}else if(unidade.intValue()==2) {
			equipe = new BigDecimal(2);
		}else if(unidade.intValue()==3) {
			equipe = new BigDecimal(3);
		}else if(unidade.intValue()==4) {
			equipe = new BigDecimal(4);
		}else if(unidade.intValue()==5) {
			equipe = new BigDecimal(5);
		}else if(unidade.intValue()==6) {
			equipe = new BigDecimal(6);
		}else if(unidade.intValue()==7) {
			equipe = new BigDecimal(7);
		}else if(unidade.intValue()==8) {
			equipe = new BigDecimal(8);
		}else {
			equipe = new BigDecimal(9);
		}
		
		return equipe;
	}
	
	 public BigDecimal getBigDecimal( Object value ) {
	        BigDecimal ret = null;
	        if( value != null ) {
	            if( value instanceof BigDecimal ) {
	                ret = (BigDecimal) value;
	            } else if( value instanceof String ) {
	                ret = new BigDecimal( (String) value );
	            } else if( value instanceof BigInteger ) {
	                ret = new BigDecimal( (BigInteger) value );
	            } else if( value instanceof Number ) {
	                ret = new BigDecimal( ((Number)value).doubleValue() );
	            } else {
	                System.out.println("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");
	            }
	        }
	        return ret;
	    }
}
