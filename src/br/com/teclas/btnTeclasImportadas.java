package br.com.teclas;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btnTeclasImportadas implements AcaoRotinaJava {
	
	/**
	 * 08-08-2023 - vs 1.2 - Gabriel Nascimento - Reconstrução da ferramenta a fim de correção de erros.
	 */
	
	int cont = 0;
	
	public void doAction(ContextoAcao contexto) throws Exception {
		
		start(contexto);

		if(cont>0) {
			contexto.setMensagemRetorno("Foram Inserida(s) / Alterada(s) <b>"+cont+"</b> Tecla(s)!");
		}else {
			throw new Error("Algo deu errado, verificar no log do sistema!");
		}

	}
	
	private void start(ContextoAcao contexto) throws Exception{
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> colecao = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("AD_TECLASIMPORTADAS", " INSERIDA = ? ", new Object[] { new String("N") }));

		for (Iterator<?> Iterator = colecao.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			String patrimonio = DynamicVO.asString("CODBEM");
			BigDecimal contrato = DynamicVO.asBigDecimal("NUMCONTRATO");
			BigDecimal tecla = DynamicVO.asBigDecimal("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			
			if(!"S".equals(validaSeEhMicroMarketing(patrimonio))){ //Máquinas normais
				if(tecla.intValue()==0) {
					throw new Error("<br/> O patrimônio "+patrimonio+" não é uma loja, não pode ser inserida tecla 0");
				}
				
				ajustesMaquina(contrato, patrimonio, tecla, DynamicVO);
				
			}
			else { //Lojas
				
				if(tecla.intValue()!=0) { //loja porém as teclas estão cadastradas numeradas
					tecla = new BigDecimal(0);
				}
				
				ajusteLoja(contrato, patrimonio, tecla, produto, DynamicVO);
			}
			
			atualizaDados(DynamicVO, contexto);
		}
	}
	
	private void atualizaDados(DynamicVO tecla, ContextoAcao contexto) throws Exception {
		JapeWrapper teclaDAO = JapeFactory.dao("AD_TECLASIMPORTADAS");
		teclaDAO.prepareToUpdate(tecla)
		.set("INSERIDA", "S")
		.set("DTALTERACAO", TimeUtils.getNow())
		.set("CODUSU",  contexto.getUsuarioLogado())
		.update();
	}
	
	private void ajustesMaquina(BigDecimal contrato, String patrimonio, BigDecimal tecla, DynamicVO linhas) throws Exception {
		if(verificaSeExisteAhTeclaMaquina(contrato,patrimonio,tecla)) { //existe uma tecla
			if(excluirTeclaMaquina(contrato, patrimonio, tecla)) {
				cadastrarTecla(linhas, contrato, tecla, patrimonio);
			}
		}else {
			cadastrarTecla(linhas, contrato, tecla, patrimonio);
		}
	}
	
	private void ajusteLoja(BigDecimal contrato, String patrimonio, BigDecimal tecla, BigDecimal produto, DynamicVO linhas) throws Exception {
		if(verificaSeExisteAhTeclaLoja(contrato, patrimonio, produto)) {
			if(excluirTeclaLoja(contrato, patrimonio, produto)) {
				cadastrarTecla(linhas, contrato, tecla, patrimonio);
			}
		}else {
			cadastrarTecla(linhas, contrato, tecla, patrimonio);
		}
	}
	
	private boolean verificaSeExisteAhTeclaLoja(BigDecimal contrato, String codbem, BigDecimal produto) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND CODPROD=?", new Object[] { contrato, codbem, produto});
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
	
	private boolean verificaSeExisteAhTeclaMaquina(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("teclas");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODBEM=? AND TECLA=?", new Object[] { contrato, codbem, tecla});
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
	
	private boolean excluirTeclaMaquina(BigDecimal contrato, String codbem, BigDecimal tecla) throws Exception {
		boolean excluida = false;
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "NUMCONTRATO=? AND CODBEM=? AND TECLA=?",
					new Object[] { contrato, codbem, tecla }));

			excluida = true;
		} catch (Exception e) {
			salvarException("[excluirTeclaMaquina] nao foi possivel excluir a tecla: " + tecla + " do patrimonio: "
					+ codbem + "\n" + e.getMessage() + "\n" + e.getCause());
		}
		return excluida;
	}
	
	private boolean excluirTeclaLoja(BigDecimal contrato, String codbem, BigDecimal produto) throws Exception {
		boolean excluida = false;
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("teclas", "NUMCONTRATO=? AND CODBEM=? AND CODPROD=?",
					new Object[] { contrato, codbem, produto }));

			excluida = true;
		} catch (Exception e) {
			salvarException("[excluirTeclaMaquina] nao foi possivel excluir o produto: " + produto + " do patrimonio: "
					+ codbem + "\n" + e.getMessage() + "\n" + e.getCause());
		}
		return excluida;
	}
	
	private String validaSeEhMicroMarketing(String patrimonio) throws Exception {
		String micromarketing = "N";
		
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		
		if(VO!=null) {
			 micromarketing = VO.asString("TOTEM");
		}

		return micromarketing;
	}
	
	private void cadastrarTecla(DynamicVO linhas,BigDecimal contrato, BigDecimal tecla, String patrimonio) throws Exception{
		
		BigDecimal produto = linhas.asBigDecimal("CODPROD");
		BigDecimal vlrpar = BigDecimalUtil.getValueOrZero(linhas.asBigDecimal("VLRPARC"));
		BigDecimal vlrfun = BigDecimalUtil.getValueOrZero(linhas.asBigDecimal("VLRFUNC"));
		BigDecimal capacidade = BigDecimalUtil.getValueOrZero(linhas.asBigDecimal("CAPACIDADE"));
		BigDecimal nivelpar = BigDecimalUtil.getValueOrZero(linhas.asBigDecimal("NIVELPAR"));
		BigDecimal nivelalerta = BigDecimalUtil.getValueOrZero(linhas.asBigDecimal("NIVELALERTA"));
		String teclaAlternativa = linhas.asString("TECLAALT");
		
		if(capacidade.intValue()==0) {
			capacidade = new BigDecimal(1);
		}
		
		if(nivelpar.intValue()==0) {
			nivelpar = new BigDecimal(1);
		}
	
		JapeWrapper teclaDAO = JapeFactory.dao("teclas");	
		DynamicVO save = teclaDAO.create()
		.set("NUMCONTRATO", contrato)
		.set("CODBEM", patrimonio)
		.set("TECLA", tecla)
		.set("CODPROD", produto)
		.set("VLRPAR", vlrpar)
		.set("VLRFUN", vlrfun)
		.set("AD_CAPACIDADE", capacidade)
		.set("AD_NIVELPAR", nivelpar)
		.set("AD_NIVELALERTA", nivelalerta)
		.save();
		
		if(teclaAlternativa!=null) {
			teclaDAO.prepareToUpdate(save).set("TECLAALT", teclaAlternativa).update();
		}
		
		this.cont++;
	}
	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btnTeclasImportadas");
			VO.setProperty("PACOTE", "br.com.teclas");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
	
}

