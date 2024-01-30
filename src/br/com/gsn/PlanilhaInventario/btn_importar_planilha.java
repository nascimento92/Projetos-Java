package br.com.gsn.PlanilhaInventario;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class btn_importar_planilha implements AcaoRotinaJava{
	
	/**
	 * Da onde eles exportam o estoque para jogar na planilha ?
	 * A contagem é salva na tabela TGFCTE campos:
	 * (K)DTCONTAGEM 
	 * (K)CODEMP
	 * (K)CODLOCAL
	 * (K)CODPROD
	 * (K)CONTROLE=''
	 * QTDEST
	 * (K)CODVOL
	 * (K)CODPARC
	 * (K)TIPO
	 * DTVAL
	 * (K)SEQUENCIA =2
	 * QTDESTUNCAD
	 * 
	 * criado a tabela AD_PLANINVENT
	 * 
	 * (K)DTCONTAGEM 
	 * (K)CODEMP
	 * (K)CODLOCAL
	 * (K)CODPROD
	 * (K)CONTROLE=NVL('')
	 * (K)CODVOL
	 * (K)CODPARC
	 * (K)TIPO
	 * (K)SEQUENCIA = 2
	 * DTUPLOAD
	 * DTATUALIZACAO
	 * USUARIOUPLOAD
	 * USUARIOATUALIZACAO
	 * OBSERVACAO
	 * QUANTIDADE
	 * DTVAL
	 * DTFABRICACAO
	 */
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		/*
		 * Timer timer = new Timer(1000, new ActionListener() {
		 * 
		 * @Override public void actionPerformed(ActionEvent e) { chamaPentaho(); } });
		 * timer.setRepeats(false); timer.start();
		 */
		
		chamaPentaho();
		
		arg0.setMensagemRetorno("<br/><br/> <b>Atenção !</b> <br/><br/> A rotina demora alguns segundos para realizar a importação ! <br/> Atualize a página para visualizar os dados ! <br/><br/><br/>");
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Google/";
			String objName = "J-Leitura_planilha";

			si.runJob(path, objName);

		} catch (Exception e) {
			throw new Error("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	/*
	 * //obrigatório ser .xls public void lerAhPlanilha(ContextoAcao arg0) { File
	 * repo = SWRepositoryUtils.getBaseFolder(); File workFolder = new File(repo,
	 * "PlanilhaInventario");
	 * 
	 * try { File f = new File(workFolder, "Notificacao.xls"); if (f.exists()) {
	 * Workbook workbook = Workbook.getWorkbook(f); Sheet sheet =
	 * workbook.getSheet(0); int linhas = sheet.getRows();
	 * 
	 * Timestamp dtcontagem = null; BigDecimal codemp = null; BigDecimal codlocal =
	 * null; BigDecimal codproduto = null; String controle = " "; String volume =
	 * "UN"; BigDecimal codparc = null; Timestamp dtval = null; Timestamp dtfabric =
	 * null; String tipo = ""; BigDecimal contagem = null;
	 * 
	 * for(int i = 0; i < linhas; i++){
	 * 
	 * Cell a1 = sheet.getCell(0, i);
	 * 
	 * if(a1.getRow()>0) { //Data contagem
	 * 
	 * String dt = a1.getContents(); if(dt!=null) { dtcontagem = new
	 * Timestamp(TimeUtils.toDate(dt)); }
	 * 
	 * //Código empresa Cell b1 = sheet.getCell(1, i); String cemp =
	 * b1.getContents(); if(cemp!=null) { codemp = new BigDecimal(cemp); }
	 * 
	 * //Código Local Cell c1 = sheet.getCell(2, i); String clocal =
	 * c1.getContents(); if(clocal!=null) { codlocal = new BigDecimal(clocal); }
	 * 
	 * //código Produto Cell d1 = sheet.getCell(3, i); String cproduto =
	 * d1.getContents(); if(cproduto!=null) { codproduto = new BigDecimal(cproduto);
	 * }
	 * 
	 * //controle Cell e1 = sheet.getCell(4, i); String ct = e1.getContents();
	 * if(ct!=null && ct!="") { controle = ct; }
	 * 
	 * //Código Volume Cell f1 = sheet.getCell(5, i); String vl = f1.getContents();
	 * if(vl!=null) { volume = vl; }
	 * 
	 * //código Parceiro Cell g1 = sheet.getCell(6, i); String cparceiro =
	 * g1.getContents(); if(cparceiro!=null) { codparc = new BigDecimal(cparceiro);
	 * }
	 * 
	 * //data Validade Cell h1 = sheet.getCell(7, i); String dtv = h1.getContents();
	 * if(dtv!=null) { dtval = new Timestamp(TimeUtils.toDate(dtv)); }
	 * 
	 * //data fabricacao Cell i1 = sheet.getCell(8, i); String dtf =
	 * i1.getContents(); if(dtf!=null) { dtfabric = new
	 * Timestamp(TimeUtils.toDate(dtf)); }
	 * 
	 * //Tipo (próprio, terceiro) Cell j1 = sheet.getCell(9, i); String tp =
	 * j1.getContents(); if(tp!=null) { tipo = tp; }
	 * 
	 * //contagem Cell k1 = sheet.getCell(10, i); String cont = k1.getContents();
	 * if(cont!=null) { contagem = new BigDecimal(cont); }
	 * 
	 * //validar qual data esta sendo salva vazia!.
	 * 
	 * //insereNaTela(dtcontagem, codemp, codlocal, codproduto, controle, volume,
	 * codparc, tipo, dtval, dtfabric, contagem); }
	 * 
	 * }
	 * 
	 * //arg0.setMensagemRetorno("Linhas: "+linhas); } } catch (Exception e) {
	 * arg0.setMensagemRetorno(e.getMessage()+"\n"+e.getCause()); } }
	 * 
	 * public void insereNaTela(Timestamp dtcontagem, BigDecimal codemp, BigDecimal
	 * codlocal, BigDecimal codproduto, String controle, String volume, BigDecimal
	 * codparc, String tipo, Timestamp dtval, Timestamp dtfabric, BigDecimal
	 * contagem) { try { EntityFacade dwfFacade =
	 * EntityFacadeFactory.getDWFFacade(); EntityVO NPVO =
	 * dwfFacade.getDefaultValueObjectInstance("AD_PLANINVENT"); DynamicVO VO =
	 * (DynamicVO) NPVO;
	 * 
	 * if(dtcontagem!=null) { VO.setProperty("DTCONTAGEM", dtcontagem); }
	 * 
	 * if(codemp!=null) { VO.setProperty("CODEMP", codemp); }
	 * 
	 * if(codlocal!=null) { VO.setProperty("CODLOCAL", codlocal); }
	 * 
	 * if(codproduto!=null) { VO.setProperty("CODPROD", codproduto); }
	 * 
	 * if(controle!=null) { VO.setProperty("CONTROLE", controle); }
	 * 
	 * if(volume!=null) { VO.setProperty("CODVOL", volume); }
	 * 
	 * if(codparc!=null) { VO.setProperty("CODPARC", codparc); }
	 * 
	 * if(tipo!=null) { VO.setProperty("TIPO", tipo); }
	 * 
	 * VO.setProperty("SEQUENCIA", new BigDecimal(2)); VO.setProperty("DTUPLOAD",
	 * TimeUtils.getNow()); VO.setProperty("USUARIOUPLOAD",
	 * ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).
	 * getUserID());
	 * 
	 * if(contagem!=null) { VO.setProperty("QUANTIDADE", contagem); }
	 * 
	 * if(dtval!=null) { VO.setProperty("DTVAL", dtval); }
	 * 
	 * if(dtfabric!=null) { VO.setProperty("DTFABRICACAO", dtfabric); }
	 * 
	 * dwfFacade.createEntity("AD_PLANINVENT", (EntityVO) VO); } catch (Exception e)
	 * { throw new Error(e.getMessage()+"\n"+e.getCause()); } }
	 */
	

}
