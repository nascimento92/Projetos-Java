package br.com.flow.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnGeraNotaFlow implements AcaoRotinaJava {
	
	final BigDecimal usuario = new BigDecimal(2201);
	final BigDecimal top = new BigDecimal(1155);
	final BigDecimal tipNeg = new BigDecimal(50);
	 
	private EntityFacade dwfFacade;
	
	public void doAction(ContextoAcao contexto) throws Exception {
		
		BigDecimal nunota = null;
		
		BigDecimal Nota = criaReq(nunota);
		
		contexto.setMensagemRetorno("NOTA GERADA: "+Nota);
	}
	
	private BigDecimal criaReq(BigDecimal nunota)throws Exception {

		BigDecimal nuNotaModelo = new BigDecimal(130104892);
		System.out.println("NOTA MODELO ------------------>>>" + nuNotaModelo);

		BigDecimal codUsu = usuario;

		if (nunota == null) {
			
			try {
				
				nunota = criaCabecalho(nuNotaModelo);
				
				PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("CabecalhoNota", nunota);
				EntityVO NVO = persistentLocalEntity.getValueObject();
				DynamicVO NotaGeradaVO = (DynamicVO) NVO;
				
				//pegando as info da top
				DynamicVO topRVO = ComercialUtils.getTipoOperacao(top);
				Date dhtipoper = topRVO.asTimestamp("DHALTER");
				String tipoMovimento = topRVO.asString("TIPMOV");
				
				//pegando as info do tipo de neg
				DynamicVO tipNEG = ComercialUtils.getTipoNegociacao(tipNeg);
				Timestamp dhtipvenda = tipNEG.asTimestamp("DHALTER");

				NotaGeradaVO.setProperty("CIF_FOB", "C");
				NotaGeradaVO.setProperty("CODUSU", codUsu);
				NotaGeradaVO.setProperty("CODCENCUS", new BigDecimal(1030000));
				NotaGeradaVO.setProperty("CODEMP", new BigDecimal(2)); //fixo por enquanto
				NotaGeradaVO.setProperty("CODNAT", new BigDecimal(130000));
				NotaGeradaVO.setProperty("CODPARC", new BigDecimal(1)); //fixo por enquanto
				NotaGeradaVO.setProperty("CODPARCTRANSP", new BigDecimal(0));
				NotaGeradaVO.setProperty("CODTIPOPER", top);
				NotaGeradaVO.setProperty("CODTIPVENDA", tipNeg);
				NotaGeradaVO.setProperty("NUMCONTRATO", new BigDecimal(1314));//fixo por enquanto
				NotaGeradaVO.setProperty("OBSERVACAO", "OBSERVACAO TESTE");
				NotaGeradaVO.setProperty("DHTIPOPER", dhtipoper);
				NotaGeradaVO.setProperty("PENDENTE", new String("S"));
				NotaGeradaVO.setProperty("TIPMOV", tipoMovimento);
				NotaGeradaVO.setProperty("DHTIPVENDA", dhtipvenda);
				
				persistentLocalEntity.setValueObject(NVO);
				
			} catch (Exception e) {
				System.out.println("Não foi possivel gerar o cabeçaho da nota!"+e.getMessage());
			}
		}
		return nunota;
	}
	
	public BigDecimal criaCabecalho(BigDecimal nuModelo) throws Exception {

		BigDecimal nunota = new java.math.BigDecimal(0);

		try {

			BigDecimal nuNotaModelo = (BigDecimal) nuModelo;

			dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;

			BigDecimal codTipOper = ModeloNPVO.asBigDecimal("CODTIPOPER");

			DynamicVO topRVO = ComercialUtils.getTipoOperacao(codTipOper);
			String tipoMovimento = topRVO.asString("TIPMOV");

			NotaProdVO.setProperty("CODTIPOPER", codTipOper);
			NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
			NotaProdVO.setProperty("CODPARC", ModeloNPVO.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("CODTIPVENDA", ModeloNPVO.asBigDecimal("CODTIPVENDA"));
			NotaProdVO.setProperty("CODEMP", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
			NotaProdVO.setProperty("CODCENCUS", ModeloNPVO.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			NotaProdVO.setProperty("PENDENTE", "N");
			NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB"));
			NotaProdVO.setProperty("CODEMPNEGOC", ModeloNPVO.asBigDecimal("CODEMP"));
			NotaProdVO.setProperty("TIPFRETE", "N");

			dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO);
			nunota = NotaProdVO.asBigDecimal("NUNOTA");

			return nunota;

		} catch (Exception e) {
			System.out.println("Problema ao criar cabecalho!!"+e.getMessage());
			e.printStackTrace();
		}
		return nunota;
	}
	
}
