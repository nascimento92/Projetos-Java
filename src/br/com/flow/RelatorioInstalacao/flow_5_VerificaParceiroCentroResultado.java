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

public class flow_5_VerificaParceiroCentroResultado implements TarefaJava {
	
	/**
	 * Não utilizado
	 */
	private String tarefa = "UserTask_1xueuc0";
	private String processo = "5";
	
	public void executar(ContextoTarefa contexto) throws Exception {
		
		start(contexto);
	}
	
	private void start(ContextoTarefa contexto) throws Exception {
		Object contrato = contexto.getCampo("CD_CONTRATO");
		contexto.setCampo("CD_PARCEIRO", getParceiro(contrato));
		
		Object centroResultado = contexto.getCampo("CT_CODCENCUS");
		
		if(validaSeExisteRegistro()) {
			atualizaRegistro(centroResultado);
		}
	}
	
	private String getParceiro(Object contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		
		BigDecimal parceiro = VO.asBigDecimal("CODPARC");
		
		return parceiro.toString();
	}
	
	private boolean validaSeExisteRegistro() throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("UsuarioCandidatoProcesso");
		DynamicVO VO = DAO.findOne("IDELEMENTO=? AND CODPRN=?",new Object[] { tarefa,processo });
		if(VO!=null) {valida = true;}
		return valida;
	}
	
	private void atualizaRegistro(Object centroResultado) throws Exception {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("UsuarioCandidatoProcesso",
					"this.IDELEMENTO=? AND this.CODPRN=? ", new Object[] {tarefa, processo}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				BigDecimal equipe = validaCentroResultado(centroResultado);
				
				VO.setProperty("CODEQUIPE", equipe);

				itemEntity.setValueObject(NVO);

			}
			
		} catch (Exception e) {
			System.out.println("Nao foi possivel alterar o centro de resultado !"+e.getMessage());
		}
		
	}
	
	private BigDecimal validaCentroResultado(Object centroResultado) {
		BigDecimal centro = getBigDecimal(centroResultado);
		BigDecimal equipe = null;

		System.out.println("CENTRO : "+centro);
		
		if(centro.intValue()==1010101) {
			equipe = new BigDecimal(10);
		}else if(centro.intValue()==1010103) {
			equipe = new BigDecimal(10);
		}else if(centro.intValue()==1010104) {
			equipe = new BigDecimal(10);
		}else if(centro.intValue()==1010113) {
			equipe = new BigDecimal(12);
		}else if(centro.intValue()==1010201) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010202) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010203) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010301) {
			equipe = new BigDecimal(13);
		}else if(centro.intValue()==1010302) {
			equipe = new BigDecimal(13);
		}else if(centro.intValue()==1010312) {
			equipe = new BigDecimal(13);
		}else if(centro.intValue()==1010501) {
			equipe = new BigDecimal(12);
		}else if(centro.intValue()==1010502) {
			equipe = new BigDecimal(12);
		}else if(centro.intValue()==1010503) {
			equipe = new BigDecimal(12);
		}else if(centro.intValue()==1010512) {
			equipe = new BigDecimal(12);
		}else if(centro.intValue()==1010601) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010602) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010603) {
			equipe = new BigDecimal(11);
		}else if(centro.intValue()==1010610) {
			equipe = new BigDecimal(11);
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
