package br.com.teclas;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnTeclasImportadas implements AcaoRotinaJava {
	
	private EntityFacade dwfEntityFacade = null;
	
	public void doAction(ContextoAcao contexto) throws Exception {
		
		start(contexto);
		contexto.setMensagemRetorno("Teclas alteradas!");

	}
	
	private void start(ContextoAcao contexto) throws Exception{
		
		try {
			
			dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> colecao = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_TECLASIMPORTADAS"," INSERIDA = ? ", new Object[] { new String("N")}));

			for (Iterator<?> Iterator = colecao.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String codbem = DynamicVO.asString("CODBEM");
			BigDecimal contrato = DynamicVO.asBigDecimal("NUMCONTRATO");
			BigDecimal tecla = DynamicVO.asBigDecimal("TECLA");
			
			if(verificaSeExisteAhTecla(contrato,codbem,tecla)){
				
				alterarTecla(DynamicVO, contrato,tecla,codbem);
				
				DynamicVO.setProperty("INSERIDA", "S");
				DynamicVO.setProperty("DTALTERACAO", new Timestamp(System.currentTimeMillis()));
				DynamicVO.setProperty("CODUSU", contexto.getUsuarioLogado());
				
				itemEntity.setValueObject((EntityVO) DynamicVO);
				
			}else{
				
				cadastrarTecla(DynamicVO);
				
				DynamicVO.setProperty("INSERIDA", "S");
				DynamicVO.setProperty("DTALTERACAO", new Timestamp(System.currentTimeMillis()));
				DynamicVO.setProperty("CODUSU", contexto.getUsuarioLogado());
				
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}
				
			}
			
		} catch (Exception e) {
			System.out.println("## [br.com.teclas.btnTeclasImportadas] ## - NAO FOI POSSIVEL CADASTRAR/ALTERAR AS TECLAS!"+e.getMessage());
			e.printStackTrace();
		}
		
		
	}
	
	private boolean verificaSeExisteAhTecla(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception{
		boolean existe = false;
		
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND TECLA=?", new Object[] { contrato,codbem,tecla });

		if (VO!=null){
			return existe = true;
		}
		
		return existe;
	}
	
	private void alterarTecla(DynamicVO teclasVO, BigDecimal contrato, BigDecimal tecla, String codbem) throws Exception{
		
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> colecao = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("teclas","this.NUMCONTRATO=? AND this.TECLA=? AND this.CODBEM=? ", new Object[] { contrato,tecla,codbem }));

		for (Iterator<?> Iterator = colecao.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("CODPROD", teclasVO.asBigDecimal("CODPROD"));
			VO.setProperty("VLRPAR", teclasVO.asBigDecimal("VLRPARC"));
			VO.setProperty("VLRFUN", teclasVO.asBigDecimal("VLRFUNC"));
			VO.setProperty("AD_CAPACIDADE", teclasVO.asBigDecimal("CAPACIDADE"));
			
			if(teclasVO.asBigDecimal("NIVELPAR").intValue()>0) {
				VO.setProperty("AD_NIVELPAR", teclasVO.asBigDecimal("NIVELPAR"));
			}
			
			if(teclasVO.asBigDecimal("NIVELALERTA").intValue()>0) {
				VO.setProperty("AD_NIVELALERTA",teclasVO.asBigDecimal("NIVELALERTA"));
			}
			
			String teclaAlternativa = teclasVO.asString("TECLAALT");
			if(teclaAlternativa!=null){
				VO.setProperty("TECLAALT", teclaAlternativa);
			}
			

			itemEntity.setValueObject((EntityVO) VO);
		}

	}
	
	private void cadastrarTecla(DynamicVO teclasVO) throws Exception{
		
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfEntityFacade.getDefaultValueObjectInstance("teclas");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("VLRPAR", teclasVO.asBigDecimal("VLRPARC"));
		VO.setProperty("VLRFUN", teclasVO.asBigDecimal("VLRFUNC"));
		VO.setProperty("TECLA", teclasVO.asBigDecimal("TECLA"));
		VO.setProperty("CODBEM", teclasVO.asString("CODBEM"));
		VO.setProperty("NUMCONTRATO", teclasVO.asBigDecimal("NUMCONTRATO"));
		VO.setProperty("CODPROD", teclasVO.asBigDecimal("CODPROD"));
			
		VO.setProperty("AD_CAPACIDADE", teclasVO.asBigDecimal("CAPACIDADE"));
		VO.setProperty("AD_NIVELPAR", teclasVO.asBigDecimal("NIVELPAR"));
		VO.setProperty("AD_NIVELALERTA", teclasVO.asBigDecimal("NIVELALERTA"));
		VO.setProperty("VALIDAVALOR", "S");
		VO.setProperty("PADRAO", "P");
		
		String teclaAlternativa = teclasVO.asString("TECLAALT");
		if(teclaAlternativa!=null){
			VO.setProperty("TECLAALT", teclaAlternativa);
		}

		dwfEntityFacade.createEntity("teclas", (EntityVO) VO);

	}
}

