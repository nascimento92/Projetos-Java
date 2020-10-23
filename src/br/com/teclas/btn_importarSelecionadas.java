package br.com.teclas;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_importarSelecionadas implements AcaoRotinaJava {
	/**
	 * 22/10/20 09:10 - Criado para importar apenas as teclas selecionadas, AD_TECLASIMPORTADAS (Teclas Importadas).
	 */
	private EntityFacade dwfEntityFacade = null;
	int cont=0;
	String causa = "";
	
	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		start(contexto);
		
		if(cont>0) {
			contexto.setMensagemRetorno("Foram Inserida(s) / Alterada(s) <b>"+cont+"</b> Tecla(s)!");
		}else {
			throw new Error("Algo deu errado, verificar no log do sistema!"+ this.causa);
		}
		
	}
	
	private void start(ContextoAcao contexto) throws Exception{
		Registro[] linhas = contexto.getLinhas();
		
		for(int i=0; i<linhas.length; i++) {
			inserirTecla(linhas[i], contexto);
		}
	}
	
	private void inserirTecla(Registro linhas,ContextoAcao contexto) throws Exception {
		String patrimonio = (String) linhas.getCampo("CODBEM");
		BigDecimal contrato = (BigDecimal) linhas.getCampo("NUMCONTRATO");
		BigDecimal tecla = (BigDecimal) linhas.getCampo("TECLA");
		
		if(verificaSeExisteAhTecla(contrato,patrimonio,tecla)) { //alterar
			
			alterarTecla(linhas, contrato,tecla,patrimonio);
			
		}else { //cadastrar
			
			cadastrarTecla(linhas, contrato,tecla,patrimonio);
		}
		
		if(cont>0) {
			linhas.setCampo("INSERIDA", "S");
			linhas.setCampo("DTALTERACAO", TimeUtils.getNow());
			linhas.setCampo("CODUSU", contexto.getUsuarioLogado());
		}
	}
	
	private boolean verificaSeExisteAhTecla(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND TECLA=?", new Object[] { contrato, codbem, tecla });
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
	
	private void alterarTecla(Registro linhas, BigDecimal contrato, BigDecimal tecla, String patrimonio) throws Exception{
		
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		BigDecimal vlrpar = (BigDecimal) linhas.getCampo("VLRPARC");
		BigDecimal vlrfun = (BigDecimal) linhas.getCampo("VLRFUNC");
		BigDecimal capacidade = (BigDecimal) linhas.getCampo("CAPACIDADE");
		BigDecimal nivelpar = (BigDecimal) linhas.getCampo("NIVELPAR");
		BigDecimal nivelalerta = (BigDecimal) linhas.getCampo("NIVELALERTA");
		String teclaAlternativa = (String) linhas.getCampo("TECLAALT");
		
		try {
			
			dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> colecao = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("teclas","this.NUMCONTRATO=? AND this.TECLA=? AND this.CODBEM=? ", new Object[] { contrato,tecla,patrimonio }));

			for (Iterator<?> Iterator = colecao.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODPROD", produto);
				VO.setProperty("VLRPAR", vlrpar);
				VO.setProperty("VLRFUN", vlrfun);
				VO.setProperty("AD_CAPACIDADE", capacidade);
				
				if(nivelpar.intValue()>0) {
					VO.setProperty("AD_NIVELPAR", nivelpar);
				}
				
				if(nivelalerta.intValue()>0) {
					VO.setProperty("AD_NIVELALERTA",nivelalerta);
				}
				
				if(teclaAlternativa!=null){
					VO.setProperty("TECLAALT", teclaAlternativa);
				}
				
				itemEntity.setValueObject((EntityVO) VO);
				cont++;
			}
			
		} catch (Exception e) {
			System.out.println("## [btn_importarSelecionadas] ## - Nao foi possivel alterar a tecla!");
			e.getMessage();
			e.getCause();
			this.causa = e.getMessage();
		}
	}
	
	private void cadastrarTecla(Registro linhas,BigDecimal contrato, BigDecimal tecla, String patrimonio) throws Exception{
		
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		BigDecimal vlrpar = (BigDecimal) linhas.getCampo("VLRPARC");
		BigDecimal vlrfun = (BigDecimal) linhas.getCampo("VLRFUNC");
		BigDecimal capacidade = (BigDecimal) linhas.getCampo("CAPACIDADE");
		BigDecimal nivelpar = (BigDecimal) linhas.getCampo("NIVELPAR");
		BigDecimal nivelalerta = (BigDecimal) linhas.getCampo("NIVELALERTA");
		String teclaAlternativa = (String) linhas.getCampo("TECLAALT");
		
		try {
			
			dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfEntityFacade.getDefaultValueObjectInstance("teclas");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("NUMCONTRATO", contrato);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("AD_CAPACIDADE", capacidade);
			VO.setProperty("VALIDAVALOR", "S");
			VO.setProperty("PADRAO", "P");
			
			if(nivelpar.intValue()>0) {
				VO.setProperty("AD_NIVELPAR", nivelpar);
			}
			
			if(nivelalerta.intValue()>0) {
				VO.setProperty("AD_NIVELALERTA",nivelalerta);
			}
						
			if(teclaAlternativa!=null){
				VO.setProperty("TECLAALT", teclaAlternativa);
			}

			dwfEntityFacade.createEntity("teclas", (EntityVO) VO);
			cont ++;
			
		} catch (Exception e) {
			System.out.println("## [btn_importarSelecionadas] ## - Nao foi possivel cadastrar a tecla!");
			e.getMessage();
			e.getCause();
			this.causa = e.getMessage();
		}
	}
}
