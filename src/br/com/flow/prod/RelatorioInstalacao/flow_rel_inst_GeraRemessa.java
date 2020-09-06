package br.com.flow.prod.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_rel_inst_GeraRemessa implements TarefaJava {
	
	/**
	 * Objeto que gera uma Nota de Remessa atraves do fluxo do flow.
	 * 
	 * @author gabriel.nascimento
	 * @versao 1.0
	 * 
	 */
	
	final BigDecimal usuario = new BigDecimal(2379);
	final BigDecimal topRemessa = new BigDecimal(1155);
	final BigDecimal topDemonstracao = new BigDecimal(1159);
	final BigDecimal tipNeg = new BigDecimal(50);
	private String idProcesso = "";
	 
	private EntityFacade dwfFacade;
	
	public void executar(ContextoTarefa arg0) throws Exception {
		
		try {
			start(arg0);
		} catch (Exception e) {
			System.out.println("NAO FOI POSSIVEL CRIAR UMA NOTA"+ e.getMessage());
		}
	}
	
	//1.0
	private void start(ContextoTarefa arg0) throws Exception {
				
		//pega os dados do processo
		String parceiro =  (String) arg0.getCampo("CD_PARCEIRO");
		String parceiroDemonstracao = (String) arg0.getCampo("CD_PARCDEMONSTRACAO");
		String contrato =  (String) arg0.getCampo("CD_CONTRATO");
		String empresa =  (String) arg0.getCampo("PL_EMPRESANOTA");
		String observacao =  (String) arg0.getCampo("SISTEMA_OBSNOTA");
		BigDecimal idInstanceProcesso =  (BigDecimal) arg0.getIdInstanceProcesso();
		idProcesso = idInstanceProcesso.toString();
		
		BigDecimal nunota = null;
		
		if(parceiroDemonstracao==null) {
			parceiroDemonstracao="0";
		}
		
		//converte os dados
		BigDecimal b_parceiro = new BigDecimal(parceiro);
		BigDecimal b_contrato = new BigDecimal(contrato);
		BigDecimal b_empresa = new BigDecimal(empresa);
		BigDecimal b_parceiroDemonstracao = null;
		if(parceiroDemonstracao!=null) {
			b_parceiroDemonstracao = new BigDecimal(parceiroDemonstracao);
		}

		nunota = criaReq(nunota,b_parceiro,b_contrato,observacao,b_empresa,b_parceiroDemonstracao);
		//verificaProdutos(idInstanceProcesso,nunota);
		
		arg0.setCampo("FT_NOTAREMESSA", nunota.toString());
	}
	
	//1.1
	private BigDecimal criaReq(BigDecimal nunota, BigDecimal parceiro, BigDecimal contrato, String observacao, BigDecimal empresa, BigDecimal parceiroDemonstracao)throws Exception {
		
		BigDecimal nuNotaModelo=null;
		
		if(parceiroDemonstracao.intValue()!=0) {
			nuNotaModelo = new BigDecimal(131360636);
		}else {
			nuNotaModelo = new BigDecimal(130104892);
		}
		
		BigDecimal codUsu = usuario;
		
		//definir a empresa de acordo com a unidade
		
		if (nunota == null) {
			
			try {
				
				nunota = criaCabecalho(nuNotaModelo);
				
				PersistentLocalEntity persistentLocalEntity = dwfFacade.findEntityByPrimaryKey("CabecalhoNota", nunota);
				EntityVO NVO = persistentLocalEntity.getValueObject();
				DynamicVO NotaGeradaVO = (DynamicVO) NVO;
								
				//pegando as info do tipo de neg
				DynamicVO tipNEG = ComercialUtils.getTipoNegociacao(tipNeg);
				Timestamp dhtipvenda = tipNEG.asTimestamp("DHALTER");

				NotaGeradaVO.setProperty("CIF_FOB", "C");
				NotaGeradaVO.setProperty("CODUSU", codUsu);
				NotaGeradaVO.setProperty("CODCENCUS", new BigDecimal(1030000));
				NotaGeradaVO.setProperty("CODEMP", empresa); //fixo por enquanto
				NotaGeradaVO.setProperty("CODNAT", new BigDecimal(130000));
				NotaGeradaVO.setProperty("CODPARCTRANSP", new BigDecimal(0));
				NotaGeradaVO.setProperty("CODTIPVENDA", tipNeg);
				
				NotaGeradaVO.setProperty("OBSERVACAO", observacao);
				NotaGeradaVO.setProperty("PENDENTE", new String("S"));
				NotaGeradaVO.setProperty("DHTIPVENDA", dhtipvenda);
				NotaGeradaVO.setProperty("AD_FLOW", idProcesso);
				
				if(parceiroDemonstracao.intValue()!=0) {
					DynamicVO topRVO = ComercialUtils.getTipoOperacao(topDemonstracao);
					Date dhtipoper = topRVO.asTimestamp("DHALTER");
					String tipoMovimento = topRVO.asString("TIPMOV");
					
					NotaGeradaVO.setProperty("CODPARC", parceiroDemonstracao); //fixo por enquanto
					NotaGeradaVO.setProperty("CODTIPOPER", topDemonstracao);
					NotaGeradaVO.setProperty("TIPMOV", tipoMovimento);
					NotaGeradaVO.setProperty("DHTIPOPER", dhtipoper);
					NotaGeradaVO.setProperty("NUMCONTRATO", new BigDecimal(0));
				}else {
					//pegando as info da top
					DynamicVO topRVO = ComercialUtils.getTipoOperacao(topRemessa);
					Date dhtipoper = topRVO.asTimestamp("DHALTER");
					String tipoMovimento = topRVO.asString("TIPMOV");
					
					NotaGeradaVO.setProperty("CODPARC", parceiro);
					NotaGeradaVO.setProperty("CODTIPOPER", topRemessa);
					NotaGeradaVO.setProperty("TIPMOV", tipoMovimento);
					NotaGeradaVO.setProperty("DHTIPOPER", dhtipoper);
					NotaGeradaVO.setProperty("NUMCONTRATO", contrato);//fixo por enquanto
				}

				persistentLocalEntity.setValueObject(NVO);
				
			} catch (Exception e) {
				System.out.println("Não foi possivel gerar o cabeçaho da nota!"+e.getMessage());
			}
		}
		return nunota;
	}
	
	
	//1.1.1
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
